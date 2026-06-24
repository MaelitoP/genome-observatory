package dev.maelitop.evolution.server;

import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.CoEvolution;
import dev.maelitop.evolution.core.evolution.CoEvolutionResult;
import dev.maelitop.evolution.core.evolution.GenerationResult;
import dev.maelitop.evolution.core.evolution.Simulation;
import dev.maelitop.evolution.core.evolution.WeightsOnlyStrategy;
import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.persistence.GenerationRecord;
import dev.maelitop.evolution.persistence.RunSpec;
import dev.maelitop.evolution.persistence.RunStore;
import dev.maelitop.evolution.persistence.StoredAgent;
import dev.maelitop.evolution.persistence.StoredRun;
import java.util.ArrayList;
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

  public long start(long seed, int generations, int carnivores) {
    long runId;
    synchronized (lock) {
      runId =
          store.startRun(
              new RunSpec(seed, config, generations, carnivores, System.currentTimeMillis()));
    }
    var _ = runner.submit(() -> execute(runId, seed, generations, carnivores));
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

  public List<GenerationRecord> generations(long id) {
    synchronized (lock) {
      return store.loadGenerationRecords(id);
    }
  }

  public List<GenerationRecord> generations(long id, Team team) {
    synchronized (lock) {
      return store.loadGenerations(id, team).stream()
          .map(stats -> new GenerationRecord(team, stats))
          .toList();
    }
  }

  public Optional<StoredAgent> agent(long id) {
    synchronized (lock) {
      return store.loadAgent(id);
    }
  }

  public Optional<Genome> champion(long id, Team team) {
    synchronized (lock) {
      return store.loadChampion(id, team);
    }
  }

  public Optional<List<TeamRunStats>> stats(long id) {
    synchronized (lock) {
      Optional<StoredRun> run = store.loadRun(id);
      if (run.isEmpty()) {
        return Optional.empty();
      }
      List<TeamRunStats> perTeam = new ArrayList<>();
      for (Team team : teamsOf(run.get())) {
        perTeam.add(new TeamRunStats(team, RunStats.of(id, store.loadGenerations(id, team))));
      }
      return Optional.of(perTeam);
    }
  }

  private static List<Team> teamsOf(StoredRun run) {
    return run.carnivores() > 0 ? List.of(Team.HERBIVORE, Team.CARNIVORE) : List.of(Team.HERBIVORE);
  }

  private void execute(long runId, long seed, int generations, int carnivores) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    try {
      if (carnivores > 0) {
        runCoEvolution(runId, rng, generations, carnivores);
      } else {
        runSingle(runId, rng, generations);
      }
    } catch (RuntimeException e) {
      log.error("run {} aborted", runId, e);
    }
  }

  private void runSingle(long runId, RandomGenerator rng, int generations) {
    Simulation simulation = new Simulation(config, rng);
    for (int g = 0; g < generations; g++) {
      GenerationResult result = simulation.runGeneration();
      synchronized (lock) {
        store.recordGeneration(runId, Team.HERBIVORE, result.stats(), result.population());
      }
    }
  }

  private void runCoEvolution(long runId, RandomGenerator rng, int generations, int carnivores) {
    CoEvolution coEvolution =
        new CoEvolution(
            config,
            rng,
            new WeightsOnlyStrategy(rng),
            new WeightsOnlyStrategy(rng),
            config.population(),
            carnivores);
    for (int g = 0; g < generations; g++) {
      CoEvolutionResult result = coEvolution.runGeneration();
      synchronized (lock) {
        store.recordGeneration(
            runId, Team.HERBIVORE, result.herbivores().stats(), result.herbivores().population());
        store.recordGeneration(
            runId, Team.CARNIVORE, result.carnivores().stats(), result.carnivores().population());
      }
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
