package dev.maelitop.evolution.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.protocol.Team;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fetched off the render thread; results published into immutable holders the renderer reads. */
final class RunApiClient implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(RunApiClient.class);

  private final HttpClient http = HttpClient.newHttpClient();
  private final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
  private final URI base;

  private final AtomicReference<List<RunSummary>> runs = new AtomicReference<>(List.of());
  private final ConcurrentMap<Long, List<GenerationRecordView>> generations =
      new ConcurrentHashMap<>();
  private final AtomicReference<Champion> champion = new AtomicReference<>();

  private record Champion(GenomeView genome, String json) {}

  private record StartRunRequest(long seed, int generations, int carnivores) {}

  RunApiClient(URI base) {
    this.base = base;
  }

  CompletableFuture<Void> refreshRuns() {
    return get("/runs")
        .thenApply(body -> read(body, RunSummary[].class))
        .thenAccept(array -> runs.set(array == null ? List.of() : List.of(array)))
        .exceptionally(ex -> warn("run list", ex));
  }

  List<RunSummary> runs() {
    return runs.get();
  }

  CompletableFuture<Void> loadGenerations(long runId) {
    return get("/runs/" + runId + "/generations")
        .thenApply(body -> read(body, GenerationRecordView[].class))
        .thenAccept(array -> generations.put(runId, array == null ? List.of() : List.of(array)))
        .exceptionally(ex -> warn("generations", ex));
  }

  List<GenerationRecordView> generations(long runId) {
    return generations.getOrDefault(runId, List.of());
  }

  CompletableFuture<Void> loadChampion(long runId, Team team) {
    champion.set(null);
    return get("/runs/" + runId + "/champion?team=" + team.name())
        .thenAccept(
            body -> {
              GenomeView view = read(body, GenomeView.class);
              if (view != null) {
                champion.set(new Champion(view, body));
              }
            })
        .exceptionally(ex -> warn("champion", ex));
  }

  Optional<GenomeView> champion() {
    Champion current = champion.get();
    return current == null ? Optional.empty() : Optional.of(current.genome());
  }

  void startRun(long seed, int generations, int carnivores) {
    String body = write(new StartRunRequest(seed, generations, carnivores));
    if (body == null) {
      return;
    }
    HttpRequest request =
        HttpRequest.newBuilder(base.resolve("/runs"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
    var _ =
        http.sendAsync(request, BodyHandlers.ofString())
            .thenAccept(
                _ -> {
                  var _ = refreshRuns();
                })
            .exceptionally(ex -> warn("start run", ex));
  }

  void exportChampion(Path target) {
    Champion current = champion.get();
    if (current == null) {
      return;
    }
    try {
      Files.writeString(target, current.json());
      log.info("exported champion genome to {}", target.toAbsolutePath());
    } catch (IOException e) {
      log.warn("failed to export champion genome", e);
    }
  }

  @Override
  public void close() {
    http.close();
  }

  private CompletableFuture<String> get(String path) {
    HttpRequest request = HttpRequest.newBuilder(base.resolve(path)).GET().build();
    return http.sendAsync(request, BodyHandlers.ofString())
        .thenApply(response -> response.statusCode() == 200 ? response.body() : null);
  }

  private <T> T read(String body, Class<T> type) {
    if (body == null) {
      return null;
    }
    try {
      return mapper.readValue(body, type);
    } catch (JsonProcessingException e) {
      log.warn("failed to parse {}", type.getSimpleName(), e);
      return null;
    }
  }

  private String write(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.warn("failed to serialize request {}", value, e);
      return null;
    }
  }

  private Void warn(String what, Throwable ex) {
    log.warn("{} request failed", what, ex);
    return null;
  }
}
