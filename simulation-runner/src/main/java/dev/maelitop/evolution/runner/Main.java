package dev.maelitop.evolution.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.CoEvolution;
import dev.maelitop.evolution.core.evolution.CoEvolutionResult;
import dev.maelitop.evolution.core.evolution.GenerationResult;
import dev.maelitop.evolution.core.evolution.GenerationStats;
import dev.maelitop.evolution.core.evolution.Simulation;
import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.persistence.GenomeCodec;
import dev.maelitop.evolution.persistence.RunSpec;
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
    WorldConfig config = WorldConfig.defaults();
    if (options.dbPath() == null) {
      if (options.coEvolution()) {
        runCoEvolution(
            config,
            options.seed(),
            options.strategy(),
            options.carnivores(),
            options.generations(),
            null,
            0);
      } else {
        run(simulation(config, options.seed(), options.strategy()), options.generations(), null, 0);
      }
      return;
    }

    ObjectMapper mapper = new ObjectMapper();
    try (RunStore store = new RunStore("jdbc:sqlite:" + options.dbPath(), mapper)) {
      long runId =
          options.replayRunId() != null
              ? replay(store, options.replayRunId(), options)
              : freshRun(store, config, options);
      if (options.exportChampionPath() != null) {
        exportChampions(store, runId, options, mapper);
      }
    }
  }

  private static long freshRun(RunStore store, WorldConfig config, RunOptions options) {
    long runId =
        store.startRun(
            new RunSpec(
                options.seed(),
                config,
                options.generations(),
                options.carnivores(),
                System.currentTimeMillis()));
    log.info(
        "seed={} generations={} strategy={} carnivores={} run={}",
        options.seed(),
        options.generations(),
        options.strategy().flag(),
        options.carnivores(),
        runId);
    if (options.coEvolution()) {
      runCoEvolution(
          config,
          options.seed(),
          options.strategy(),
          options.carnivores(),
          options.generations(),
          store,
          runId);
    } else {
      run(
          simulation(config, options.seed(), options.strategy()),
          options.generations(),
          store,
          runId);
    }
    return runId;
  }

  private static long replay(RunStore store, long originalId, RunOptions options) {
    StoredRun original =
        store
            .loadRun(originalId)
            .orElseThrow(() -> new IllegalArgumentException("no run with id " + originalId));
    long runId =
        store.startRun(
            new RunSpec(
                original.seed(),
                original.config(),
                original.generations(),
                original.carnivores(),
                System.currentTimeMillis()));
    log.info("replaying run {} from seed={} as run {}", originalId, original.seed(), runId);
    if (original.carnivores() > 0) {
      runCoEvolution(
          original.config(),
          original.seed(),
          options.strategy(),
          original.carnivores(),
          original.generations(),
          store,
          runId);
      log.info(
          "replay identical to run {}: {}",
          originalId,
          teamHistoriesMatch(store, originalId, runId));
    } else {
      run(
          simulation(original.config(), original.seed(), options.strategy()),
          original.generations(),
          store,
          runId);
      log.info(
          "replay identical to run {}: {}",
          originalId,
          store
              .loadGenerations(originalId, Team.HERBIVORE)
              .equals(store.loadGenerations(runId, Team.HERBIVORE)));
    }
    return runId;
  }

  private static boolean teamHistoriesMatch(RunStore store, long originalId, long runId) {
    for (Team team : Team.values()) {
      if (!store.loadGenerations(originalId, team).equals(store.loadGenerations(runId, team))) {
        return false;
      }
    }
    return true;
  }

  private static List<GenerationStats> run(
      Simulation simulation, int generations, RunStore store, long runId) {
    List<GenerationStats> history = new ArrayList<>(generations);
    for (int g = 0; g < generations; g++) {
      GenerationResult result = simulation.runGeneration();
      GenerationStats stats = result.stats();
      if (store != null) {
        store.recordGeneration(runId, Team.HERBIVORE, stats, result.population());
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

  private static void runCoEvolution(
      WorldConfig config,
      long seed,
      Strategy strategy,
      int carnivores,
      int generations,
      RunStore store,
      long runId) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    CoEvolution coEvolution =
        new CoEvolution(
            config,
            rng,
            strategy.create(rng),
            strategy.create(rng),
            config.population(),
            carnivores);
    for (int g = 0; g < generations; g++) {
      CoEvolutionResult result = coEvolution.runGeneration();
      if (store != null) {
        store.recordGeneration(
            runId, Team.HERBIVORE, result.herbivores().stats(), result.herbivores().population());
        store.recordGeneration(
            runId, Team.CARNIVORE, result.carnivores().stats(), result.carnivores().population());
      }
      GenerationStats herbivoreStats = result.herbivores().stats();
      GenerationStats carnivoreStats = result.carnivores().stats();
      log.info(
          "gen={} herbivore[best={} mean={}] carnivore[best={} mean={}]",
          herbivoreStats.generation(),
          String.format(Locale.ROOT, "%.2f", herbivoreStats.bestFitness()),
          String.format(Locale.ROOT, "%.2f", herbivoreStats.meanFitness()),
          String.format(Locale.ROOT, "%.2f", carnivoreStats.bestFitness()),
          String.format(Locale.ROOT, "%.2f", carnivoreStats.meanFitness()));
    }
  }

  private static void exportChampions(
      RunStore store, long runId, RunOptions options, ObjectMapper mapper) {
    GenomeCodec codec = new GenomeCodec(mapper);
    if (options.coEvolution()) {
      for (Team team : Team.values()) {
        Genome champion =
            store
                .loadChampion(runId, team)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "run " + runId + " has no " + team + " agents to export"));
        Path out = teamPath(options.exportChampionPath(), team);
        codec.write(champion, out);
        log.info("exported {} champion of run {} to {}", team, runId, out);
      }
    } else {
      Genome champion =
          store
              .loadOverallChampion(runId)
              .orElseThrow(
                  () -> new IllegalStateException("run " + runId + " has no agents to export"));
      codec.write(champion, Path.of(options.exportChampionPath()));
      log.info("exported champion of run {} to {}", runId, options.exportChampionPath());
    }
  }

  private static Path teamPath(String base, Team team) {
    String suffix = team.name().toLowerCase(Locale.ROOT);
    int dot = base.lastIndexOf('.');
    return dot < 0
        ? Path.of(base + "." + suffix)
        : Path.of(base.substring(0, dot) + "." + suffix + base.substring(dot));
  }

  private static Simulation simulation(WorldConfig config, long seed, Strategy strategy) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    return new Simulation(config, rng, strategy.create(rng));
  }
}
