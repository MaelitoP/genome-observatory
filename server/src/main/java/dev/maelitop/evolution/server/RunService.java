package dev.maelitop.evolution.server;

import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.GenerationResult;
import dev.maelitop.evolution.core.evolution.GenerationStats;
import dev.maelitop.evolution.core.evolution.Simulation;
import dev.maelitop.evolution.persistence.RunStore;
import dev.maelitop.evolution.persistence.StoredAgent;
import dev.maelitop.evolution.persistence.StoredRun;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Serializes all access to the single-connection {@link RunStore}. */
public final class RunService implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(RunService.class);

  private final RunStore store;
  private final WorldConfig config;
  private final ExecutorService runner = Executors.newSingleThreadExecutor();
  private final Object lock = new Object();

  public RunService(RunStore store, WorldConfig config) {
    this.store = store;
    this.config = config;
  }

  public long start(long seed, int generations) {
    long runId;
    synchronized (lock) {
      runId = store.startRun(seed, config, generations, System.currentTimeMillis());
    }
    var _ = runner.submit(() -> execute(runId, seed, generations));
    return runId;
  }

  public List<StoredRun> runs() {
    synchronized (lock) {
      return store.listRuns();
    }
  }

  public Optional<StoredRun> run(long id) {
    synchronized (lock) {
      return store.loadRun(id);
    }
  }

  public List<GenerationStats> generations(long id) {
    synchronized (lock) {
      return store.loadGenerations(id);
    }
  }

  public Optional<StoredAgent> agent(long id) {
    synchronized (lock) {
      return store.loadAgent(id);
    }
  }

  public Optional<RunStats> stats(long id) {
    synchronized (lock) {
      if (store.loadRun(id).isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(RunStats.of(id, store.loadGenerations(id)));
    }
  }

  private void execute(long runId, long seed, int generations) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    Simulation simulation = new Simulation(config, rng);
    try {
      for (int g = 0; g < generations; g++) {
        GenerationResult result = simulation.runGeneration();
        synchronized (lock) {
          store.recordGeneration(runId, result.stats(), result.population());
        }
      }
    } catch (RuntimeException e) {
      log.error("run {} aborted", runId, e);
    }
  }

  @Override
  public void close() {
    runner.shutdown();
    try {
      if (!runner.awaitTermination(5, TimeUnit.SECONDS)) {
        runner.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      runner.shutdownNow();
    } finally {
      store.close();
    }
  }
}
