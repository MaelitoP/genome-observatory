package dev.maelitop.evolution.runner;

import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.GenerationStats;
import dev.maelitop.evolution.core.evolution.Simulation;
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
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(options.seed());
    Simulation simulation = new Simulation(WorldConfig.defaults(), rng);

    log.info("seed={} generations={}", options.seed(), options.generations());
    for (int g = 0; g < options.generations(); g++) {
      GenerationStats stats = simulation.runGeneration();
      log.info(
          "gen={} best={} mean={} median={} diversity={} pop={}",
          stats.generation(),
          String.format(Locale.ROOT, "%.2f", stats.bestFitness()),
          String.format(Locale.ROOT, "%.2f", stats.meanFitness()),
          String.format(Locale.ROOT, "%.2f", stats.medianFitness()),
          String.format(Locale.ROOT, "%.3f", stats.diversity()),
          stats.population());
    }
  }
}
