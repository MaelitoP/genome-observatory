package dev.maelitop.evolution.core.evolution;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.domain.WorldConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class FitnessImprovementTest {

  private static final long SEED = 42L;
  private static final int GENERATIONS = 30;
  private static final int WINDOW = 5;

  @Test
  void fitnessImprovesOverGenerations() {
    List<GenerationStats> history = run(SEED);
    double early = meanBest(history.subList(0, WINDOW));
    double late = meanBest(history.subList(GENERATIONS - WINDOW, GENERATIONS));

    assertThat(late).isGreaterThan(early);
  }

  private static double meanBest(List<GenerationStats> window) {
    return window.stream().mapToDouble(GenerationStats::bestFitness).average().orElseThrow();
  }

  private static List<GenerationStats> run(long seed) {
    Simulation simulation =
        new Simulation(
            WorldConfig.defaults(), RandomGeneratorFactory.of("L64X128MixRandom").create(seed));
    List<GenerationStats> history = new ArrayList<>();
    for (int g = 0; g < GENERATIONS; g++) {
      history.add(simulation.runGeneration().stats());
    }
    return history;
  }
}
