package dev.maelitop.evolution.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.maelitop.evolution.protocol.Team;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunApiClientTest {

  private HttpServer server;
  private RunApiClient api;
  private volatile int status = 200;
  private volatile String body = "[]";

  @BeforeEach
  void setUp() throws IOException {
    InetAddress loopback = InetAddress.getLoopbackAddress();
    server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
    server.createContext(
        "/",
        exchange -> {
          byte[] payload = body.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, payload.length == 0 ? -1 : payload.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
          }
        });
    server.start();
    api =
        new RunApiClient(
            URI.create(
                "http://" + loopback.getHostAddress() + ":" + server.getAddress().getPort()));
  }

  @AfterEach
  void tearDown() {
    api.close();
    server.stop(0);
  }

  @Test
  void populatesRunsOnSuccess() throws Exception {
    status = 200;
    body = "[{\"id\":7,\"seed\":1,\"generations\":10,\"carnivores\":0,\"startedAtEpochMs\":0}]";

    api.refreshRuns().get(5, TimeUnit.SECONDS);

    assertThat(api.runs()).hasSize(1);
    assertThat(api.runs().get(0).id()).isEqualTo(7L);
  }

  @Test
  void keepsRunsEmptyOnErrorStatus() throws Exception {
    status = 404;
    body = "not found";

    api.refreshRuns().get(5, TimeUnit.SECONDS);

    assertThat(api.runs()).isEmpty();
  }

  @Test
  void keepsRunsEmptyOnMalformedJson() throws Exception {
    status = 200;
    body = "{ this is not json";

    api.refreshRuns().get(5, TimeUnit.SECONDS);

    assertThat(api.runs()).isEmpty();
  }

  @Test
  void championAbsentOnErrorStatus() throws Exception {
    status = 404;
    body = "";

    api.loadChampion(1, Team.HERBIVORE).get(5, TimeUnit.SECONDS);

    assertThat(api.champion()).isEmpty();
  }

  @Test
  void exportsLoadedChampion(@TempDir Path dir) throws Exception {
    status = 200;
    body = "{\"nodes\":[{\"id\":0,\"type\":\"INPUT\"}],\"connections\":[]}";
    api.loadChampion(1, Team.HERBIVORE).get(5, TimeUnit.SECONDS);
    assertThat(api.champion()).isPresent();

    Path target = dir.resolve("champion.json");
    api.exportChampion(target);

    assertThat(Files.readString(target)).isEqualTo(body);
  }

  @Test
  void exportIsNoOpWithoutChampion(@TempDir Path dir) {
    Path target = dir.resolve("champion.json");

    api.exportChampion(target);

    assertThat(target).doesNotExist();
  }
}
