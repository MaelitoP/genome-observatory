package dev.maelitop.evolution.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.domain.FitnessWeights;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.CoEvolution;
import dev.maelitop.evolution.core.evolution.CoEvolutionResult;
import dev.maelitop.evolution.core.evolution.GenerationResult;
import dev.maelitop.evolution.core.evolution.GenerationStats;
import dev.maelitop.evolution.core.evolution.Simulation;
import dev.maelitop.evolution.core.evolution.WeightsOnlyStrategy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReplayFidelityTest {

  private static final long SEED = 123L;
  private static final int GENERATIONS = 3;
  private static final int CARNIVORES = 4;

  @TempDir Path dir;

  @Test
  void replayFromStoredSeedReproducesPersistedRun() {
    WorldConfig config = WorldConfig.defaults();

    try (RunStore store = open()) {
      long original = store.startRun(new RunSpec(SEED, config, GENERATIONS, 0, 0L));
      List<GenerationStats> originalStats = simulateInto(store, original, config);

      long replay = store.startRun(new RunSpec(SEED, config, GENERATIONS, 0, 0L));
      List<GenerationStats> replayStats = simulateInto(store, replay, config);

      assertThat(replayStats).isEqualTo(originalStats);
      assertThat(store.loadGenerations(original, Team.HERBIVORE)).isEqualTo(originalStats);
    }
  }

  @Test
  void coEvolutionReplayReproducesBothTeams() {
    WorldConfig config = smallConfig();

    try (RunStore store = open()) {
      long original = store.startRun(new RunSpec(SEED, config, GENERATIONS, CARNIVORES, 0L));
      coEvolveInto(store, original, config);

      long replay = store.startRun(new RunSpec(SEED, config, GENERATIONS, CARNIVORES, 0L));
      coEvolveInto(store, replay, config);

      for (Team team : Team.values()) {
        assertThat(store.loadGenerations(replay, team))
            .isEqualTo(store.loadGenerations(original, team));
      }
    }
  }

  private static List<GenerationStats> simulateInto(
      RunStore store, long runId, WorldConfig config) {
    Simulation simulation =
        new Simulation(config, RandomGeneratorFactory.of("L64X128MixRandom").create(SEED));
    List<GenerationStats> history = new ArrayList<>(GENERATIONS);
    for (int g = 0; g < GENERATIONS; g++) {
      GenerationResult result = simulation.runGeneration();
      store.recordGeneration(runId, Team.HERBIVORE, result.stats(), result.population());
      history.add(result.stats());
    }
    return history;
  }

  private static void coEvolveInto(RunStore store, long runId, WorldConfig config) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(SEED);
    CoEvolution coEvolution =
        new CoEvolution(
            config,
            rng,
            new WeightsOnlyStrategy(rng),
            new WeightsOnlyStrategy(rng),
            config.population(),
            CARNIVORES);
    for (int g = 0; g < GENERATIONS; g++) {
      CoEvolutionResult result = coEvolution.runGeneration();
      store.recordGeneration(
          runId, Team.HERBIVORE, result.herbivores().stats(), result.herbivores().population());
      store.recordGeneration(
          runId, Team.CARNIVORE, result.carnivores().stats(), result.carnivores().population());
    }
  }

  private static WorldConfig smallConfig() {
    return new WorldConfig(
        200,
        200,
        10,
        8,
        25.0,
        5.0,
        6,
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

  private RunStore open() {
    return new RunStore("jdbc:sqlite:" + dir.resolve("runs.db"), new ObjectMapper());
  }
}
