package dev.maelitop.evolution.core.evolution;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.domain.WorldConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class SimulationReproducibilityTest {

  @Test
  void sameSeedYieldsIdenticalGenerationStats() {
    assertThat(run(42L, 3)).isEqualTo(run(42L, 3));
  }

  @Test
  void differentSeedsDiverge() {
    assertThat(run(42L, 3)).isNotEqualTo(run(7L, 3));
  }

  @Test
  void populationStaysConstant() {
    WorldConfig config = WorldConfig.defaults();

    for (GenerationStats stats : run(1L, 3)) {
      assertThat(stats.population()).isEqualTo(config.population());
      assertThat(stats.bestFitness()).isGreaterThanOrEqualTo(stats.meanFitness());
    }
  }

  private static List<GenerationStats> run(long seed, int generations) {
    Simulation simulation =
        new Simulation(
            WorldConfig.defaults(), RandomGeneratorFactory.of("L64X128MixRandom").create(seed));
    List<GenerationStats> history = new ArrayList<>();
    for (int g = 0; g < generations; g++) {
      history.add(simulation.runGeneration());
    }
    return history;
  }
}
