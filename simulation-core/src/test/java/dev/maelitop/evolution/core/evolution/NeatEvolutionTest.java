package dev.maelitop.evolution.core.evolution;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.domain.WorldConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class NeatEvolutionTest {

  private static final long SEED = 42L;
  private static final int GENERATIONS = 30;
  private static final int WINDOW = 5;

  @Test
  void sameSeedYieldsIdenticalStats() {
    assertThat(neat(SEED, 3)).isEqualTo(neat(SEED, 3));
  }

  @Test
  void fitnessImprovesOverGenerations() {
    List<GenerationStats> history = neat(SEED, GENERATIONS);
    double early = meanBest(history.subList(0, WINDOW));
    double late = meanBest(history.subList(GENERATIONS - WINDOW, GENERATIONS));

    assertThat(late).isGreaterThan(early);
  }

  @Test
  void divergesFromWeightsBaselineOnTheSameSeed() {
    assertThat(neat(SEED, 5)).isNotEqualTo(weights(SEED, 5));
  }

  private static double meanBest(List<GenerationStats> window) {
    return window.stream().mapToDouble(GenerationStats::bestFitness).average().orElseThrow();
  }

  private static List<GenerationStats> neat(long seed, int generations) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    return run(new Simulation(WorldConfig.defaults(), rng, new NeatStrategy(rng)), generations);
  }

  private static List<GenerationStats> weights(long seed, int generations) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    return run(new Simulation(WorldConfig.defaults(), rng), generations);
  }

  private static List<GenerationStats> run(Simulation simulation, int generations) {
    List<GenerationStats> history = new ArrayList<>(generations);
    for (int g = 0; g < generations; g++) {
      history.add(simulation.runGeneration().stats());
    }
    return history;
  }
}
