package dev.maelitop.evolution.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.CoEvolution;
import dev.maelitop.evolution.core.evolution.CoEvolutionResult;
import dev.maelitop.evolution.core.evolution.GenerationResult;
import dev.maelitop.evolution.core.evolution.GenerationStats;
import dev.maelitop.evolution.core.evolution.Simulation;
import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.persistence.GenomeCodec;
import dev.maelitop.evolution.persistence.RunStore;
import dev.maelitop.evolution.persistence.StoredRun;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private Main() {}

  public static void main(String[] args) {
    RunOptions options = RunOptions.parse(args);
    if (options.coEvolution()) {
      runCoEvolution(WorldConfig.defaults(), options);
      return;
    }
    if (options.dbPath() == null) {
      run(
          simulation(WorldConfig.defaults(), options.seed(), options.strategy()),
          options.generations(),
          null,
          0);
      return;
    }

    ObjectMapper mapper = new ObjectMapper();
    try (RunStore store = new RunStore("jdbc:sqlite:" + options.dbPath(), mapper)) {
      long runId =
          options.replayRunId() != null
              ? replay(store, options.replayRunId(), options)
              : freshRun(store, options);
      if (options.exportChampionPath() != null) {
        Genome champion =
            store
                .loadOverallChampion(runId)
                .orElseThrow(
                    () -> new IllegalStateException("run " + runId + " has no agents to export"));
        new GenomeCodec(mapper).write(champion, Path.of(options.exportChampionPath()));
        log.info("exported champion of run {} to {}", runId, options.exportChampionPath());
      }
    }
  }

  private static long freshRun(RunStore store, RunOptions options) {
    WorldConfig config = WorldConfig.defaults();
    long runId =
        store.startRun(options.seed(), config, options.generations(), System.currentTimeMillis());
    log.info("seed={} generations={} run={}", options.seed(), options.generations(), runId);
    run(
        simulation(config, options.seed(), options.strategy()),
        options.generations(),
        store,
        runId);
    return runId;
  }

  private static long replay(RunStore store, long originalId, RunOptions options) {
    StoredRun original =
        store
            .loadRun(originalId)
            .orElseThrow(() -> new IllegalArgumentException("no run with id " + originalId));
    List<GenerationStats> originalStats = store.loadGenerations(originalId);
    long runId =
        store.startRun(
            original.seed(), original.config(), original.generations(), System.currentTimeMillis());
    log.info("replaying run {} from seed={} as run {}", originalId, original.seed(), runId);
    List<GenerationStats> replayStats =
        run(
            simulation(original.config(), original.seed(), options.strategy()),
            original.generations(),
            store,
            runId);
    log.info("replay identical to run {}: {}", originalId, replayStats.equals(originalStats));
    return runId;
  }

  private static List<GenerationStats> run(
      Simulation simulation, int generations, RunStore store, long runId) {
    List<GenerationStats> history = new ArrayList<>(generations);
    for (int g = 0; g < generations; g++) {
      GenerationResult result = simulation.runGeneration();
      GenerationStats stats = result.stats();
      if (store != null) {
        store.recordGeneration(runId, stats, result.population());
      }
      log.info(
          "gen={} best={} mean={} median={} diversity={} pop={}",
          stats.generation(),
          String.format(Locale.ROOT, "%.2f", stats.bestFitness()),
          String.format(Locale.ROOT, "%.2f", stats.meanFitness()),
          String.format(Locale.ROOT, "%.2f", stats.medianFitness()),
          String.format(Locale.ROOT, "%.3f", stats.diversity()),
          stats.population());
      history.add(stats);
    }
    return history;
  }

  private static void runCoEvolution(WorldConfig config, RunOptions options) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(options.seed());
    CoEvolution coEvolution =
        new CoEvolution(
            config,
            rng,
            options.strategy().create(rng),
            options.strategy().create(rng),
            config.population(),
            options.carnivores());
    log.info(
        "seed={} generations={} strategy={} herbivores={} carnivores={}",
        options.seed(),
        options.generations(),
        options.strategy().flag(),
        config.population(),
        options.carnivores());
    for (int g = 0; g < options.generations(); g++) {
      CoEvolutionResult result = coEvolution.runGeneration();
      log.info(
          "gen={} herbivore[best={} mean={}] carnivore[best={} mean={}]",
          result.herbivores().generation(),
          String.format(Locale.ROOT, "%.2f", result.herbivores().bestFitness()),
          String.format(Locale.ROOT, "%.2f", result.herbivores().meanFitness()),
          String.format(Locale.ROOT, "%.2f", result.carnivores().bestFitness()),
          String.format(Locale.ROOT, "%.2f", result.carnivores().meanFitness()));
    }
  }

  private static Simulation simulation(WorldConfig config, long seed, Strategy strategy) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    return new Simulation(config, rng, strategy.create(rng));
  }
}
