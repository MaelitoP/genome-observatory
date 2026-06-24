package dev.maelitop.evolution.core.evolution;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.domain.FitnessWeights;
import dev.maelitop.evolution.core.domain.WorldConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class CoEvolutionTest {

  private static final int HERBIVORES = 6;
  private static final int CARNIVORES = 4;

  @Test
  void sameSeedYieldsIdenticalHistory() {
    assertThat(run(42L, 3)).isEqualTo(run(42L, 3));
  }

  @Test
  void bothPopulationsKeepTheirSizeEachGeneration() {
    for (CoEvolutionResult result : run(1L, 3)) {
      assertThat(result.herbivores().stats().population()).isEqualTo(HERBIVORES);
      assertThat(result.carnivores().stats().population()).isEqualTo(CARNIVORES);
    }
  }

  private static List<CoEvolutionResult> run(long seed, int generations) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    CoEvolution coEvolution =
        new CoEvolution(
            smallConfig(),
            rng,
            new NeatStrategy(rng),
            new NeatStrategy(rng),
            HERBIVORES,
            CARNIVORES);
    List<CoEvolutionResult> history = new ArrayList<>(generations);
    for (int g = 0; g < generations; g++) {
      history.add(coEvolution.runGeneration());
    }
    return history;
  }

  private static WorldConfig smallConfig() {
    return new WorldConfig(
        200,
        200,
        10,
        8,
        25.0,
        5.0,
        HERBIVORES,
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
