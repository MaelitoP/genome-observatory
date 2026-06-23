package dev.maelitop.evolution.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.GenerationResult;
import dev.maelitop.evolution.core.evolution.GenerationStats;
import dev.maelitop.evolution.core.evolution.Simulation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReplayFidelityTest {

  private static final long SEED = 123L;
  private static final int GENERATIONS = 3;

  @TempDir Path dir;

  @Test
  void replayFromStoredSeedReproducesPersistedRun() {
    WorldConfig config = WorldConfig.defaults();

    try (RunStore store = open()) {
      long original = store.startRun(SEED, config, GENERATIONS, 0L);
      List<GenerationStats> originalStats = simulateInto(store, original, config);

      long replay = store.startRun(SEED, config, GENERATIONS, 0L);
      List<GenerationStats> replayStats = simulateInto(store, replay, config);

      assertThat(replayStats).isEqualTo(originalStats);
      assertThat(store.loadGenerations(original)).isEqualTo(originalStats);
    }
  }

  private static List<GenerationStats> simulateInto(
      RunStore store, long runId, WorldConfig config) {
    Simulation simulation =
        new Simulation(config, RandomGeneratorFactory.of("L64X128MixRandom").create(SEED));
    List<GenerationStats> history = new ArrayList<>(GENERATIONS);
    for (int g = 0; g < GENERATIONS; g++) {
      GenerationResult result = simulation.runGeneration();
      store.recordGeneration(runId, result.stats(), result.population());
      history.add(result.stats());
    }
    return history;
  }

  private RunStore open() {
    return new RunStore("jdbc:sqlite:" + dir.resolve("runs.db"), new ObjectMapper());
  }
}
