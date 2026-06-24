package dev.maelitop.evolution.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.domain.FitnessWeights;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.persistence.RunStore;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RestApiTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient http = HttpClient.newHttpClient();

  @TempDir Path dir;
  private RunService service;
  private Javalin app;
  private String base;

  @BeforeEach
  void startServer() {
    RunStore store = new RunStore("jdbc:sqlite:" + dir.resolve("runs.db"), new ObjectMapper());
    service = new RunService(store, smallConfig());
    app = Javalin.create();
    RestApi.register(app, service);
    app.start(0);
    base = "http://localhost:" + app.port();
  }

  @AfterEach
  void stopServer() {
    app.stop();
    service.close();
  }

  @Test
  void startsInspectsAndExportsRunsOverHttp() throws Exception {
    JsonNode created = post("/runs", "{\"seed\":7,\"generations\":2}", 201);
    long runId = created.get("id").asLong();

    awaitCondition(
        () -> get("/runs/" + runId + "/generations").size() == 2, Duration.ofSeconds(30));

    assertThat(get("/runs").size()).isEqualTo(1);

    JsonNode stats = get("/runs/" + runId + "/stats");
    assertThat(stats.size()).isEqualTo(1);
    assertThat(stats.get(0).get("team").asText()).isEqualTo("HERBIVORE");
    assertThat(stats.get(0).get("stats").get("generations").asInt()).isEqualTo(2);

    JsonNode agent = get("/agents/1");
    assertThat(agent.get("id").asLong()).isEqualTo(1);
    assertThat(agent.get("team").asText()).isEqualTo("HERBIVORE");
    assertThat(agent.get("genome").has("connections")).isTrue();

    HttpResponse<String> export = send("GET", "/export/agents/1", null);
    assertThat(export.statusCode()).isEqualTo(200);
    assertThat(export.headers().firstValue("Content-Disposition")).isPresent();
  }

  @Test
  void runsAndInspectsCoEvolutionPerTeam() throws Exception {
    JsonNode created = post("/runs", "{\"seed\":3,\"generations\":2,\"carnivores\":3}", 201);
    long runId = created.get("id").asLong();
    assertThat(created.get("carnivores").asInt()).isEqualTo(3);

    awaitCondition(
        () -> get("/runs/" + runId + "/generations").size() == 4, Duration.ofSeconds(30));

    JsonNode carnivoreGenerations = get("/runs/" + runId + "/generations?team=CARNIVORE");
    assertThat(carnivoreGenerations.size()).isEqualTo(2);
    assertThat(carnivoreGenerations.get(0).get("team").asText()).isEqualTo("CARNIVORE");
    assertThat(get("/runs/" + runId + "/stats").size()).isEqualTo(2);
    assertThat(get("/runs/" + runId + "/champion?team=HERBIVORE").has("connections")).isTrue();
  }

  @Test
  void championRequiresAValidTeam() throws Exception {
    assertThat(send("GET", "/runs/1/champion", null).statusCode()).isEqualTo(400);
    assertThat(send("GET", "/runs/1/champion?team=INVALID", null).statusCode()).isEqualTo(400);
  }

  @Test
  void unknownAgentIsNotFound() throws Exception {
    assertThat(send("GET", "/agents/99999", null).statusCode()).isEqualTo(404);
  }

  @Test
  void rejectsRunWithoutGenerations() throws Exception {
    assertThat(send("POST", "/runs", "{\"seed\":1,\"generations\":0}").statusCode()).isEqualTo(400);
  }

  private JsonNode get(String path) {
    try {
      HttpResponse<String> response = send("GET", path, null);
      assertThat(response.statusCode()).isEqualTo(200);
      return mapper.readTree(response.body());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private JsonNode post(String path, String body, int expectedStatus) throws Exception {
    HttpResponse<String> response = send("POST", path, body);
    assertThat(response.statusCode()).isEqualTo(expectedStatus);
    return mapper.readTree(response.body());
  }

  private HttpResponse<String> send(String method, String path, String body) throws Exception {
    HttpRequest.BodyPublisher publisher =
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body);
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(base + path))
            .header("Content-Type", "application/json")
            .method(method, publisher)
            .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static void awaitCondition(BooleanSupplier condition, Duration timeout) {
    long deadline = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      try {
        Thread.sleep(20);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
    throw new AssertionError("condition not met within " + timeout);
  }

  private static WorldConfig smallConfig() {
    return new WorldConfig(
        200,
        200,
        10,
        8,
        25.0,
        5.0,
        4,
        0.5,
        100.0,
        0.1,
        0.05,
        0.05,
        5.0,
        120.0,
        Math.PI,
        50.0,
        Math.toRadians(120),
        40.0,
        20.0,
        0.5,
        new FitnessWeights(1.0, 3.0, 0.1, 0.0));
  }
}
