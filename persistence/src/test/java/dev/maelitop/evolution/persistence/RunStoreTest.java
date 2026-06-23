package dev.maelitop.evolution.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.Evaluated;
import dev.maelitop.evolution.core.evolution.GenerationStats;
import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.Genome;
import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunStoreTest {

  private static final List<Activation> OUTPUTS =
      List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID);

  @TempDir Path dir;

  @Test
  void persistsAndReloadsRunAcrossReopen() {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(1L);
    Genome g0 = Genome.initial(14, OUTPUTS, rng);
    Genome champion = Genome.initial(14, OUTPUTS, rng);
    Genome g2 = Genome.initial(14, OUTPUTS, rng);

    long runId;
    try (RunStore store = open()) {
      runId = store.startRun(42L, WorldConfig.defaults(), 2, 1000L);
      store.recordGeneration(
          runId,
          new GenerationStats(0, 5.0, 3.0, 3.0, 0.5, 2),
          List.of(new Evaluated(g0, 1.0), new Evaluated(champion, 5.0)));
      store.recordGeneration(
          runId, new GenerationStats(1, 3.0, 2.5, 2.5, 0.4, 1), List.of(new Evaluated(g2, 3.0)));
    }

    try (RunStore store = open()) {
      assertThat(store.loadRun(runId))
          .contains(new StoredRun(runId, 42L, WorldConfig.defaults(), 2, 1000L));
      assertThat(store.loadGenerations(runId))
          .containsExactly(
              new GenerationStats(0, 5.0, 3.0, 3.0, 0.5, 2),
              new GenerationStats(1, 3.0, 2.5, 2.5, 0.4, 1));
      assertThat(store.loadOverallChampion(runId)).contains(champion);
    }
  }

  @Test
  void championTieBreakIsDeterministic() {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(2L);
    Genome first = Genome.initial(14, OUTPUTS, rng);
    Genome second = Genome.initial(14, OUTPUTS, rng);

    long runId;
    try (RunStore store = open()) {
      runId = store.startRun(7L, WorldConfig.defaults(), 1, 0L);
      store.recordGeneration(
          runId,
          new GenerationStats(0, 9.0, 9.0, 9.0, 0.0, 2),
          List.of(new Evaluated(first, 9.0), new Evaluated(second, 9.0)));
    }

    try (RunStore store = open()) {
      assertThat(store.loadOverallChampion(runId)).contains(first);
      assertThat(store.loadOverallChampion(runId)).contains(first);
    }
  }

  @Test
  void recordGenerationForMissingRunFails() {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(3L);
    try (RunStore store = open()) {
      assertThatThrownBy(
              () ->
                  store.recordGeneration(
                      404L,
                      new GenerationStats(0, 1.0, 1.0, 1.0, 0.0, 1),
                      List.of(new Evaluated(Genome.initial(14, OUTPUTS, rng), 1.0))))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("failed to record generation");
    }
  }

  @Test
  void listsRunsInInsertionOrder() {
    try (RunStore store = open()) {
      long first = store.startRun(1L, WorldConfig.defaults(), 3, 100L);
      long second = store.startRun(2L, WorldConfig.defaults(), 5, 200L);

      assertThat(store.listRuns())
          .containsExactly(
              new StoredRun(first, 1L, WorldConfig.defaults(), 3, 100L),
              new StoredRun(second, 2L, WorldConfig.defaults(), 5, 200L));
    }
  }

  @Test
  void loadsAgentByIdWithGenome() {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(4L);
    Genome only = Genome.initial(14, OUTPUTS, rng);

    try (RunStore store = open()) {
      long runId = store.startRun(8L, WorldConfig.defaults(), 1, 0L);
      store.recordGeneration(
          runId, new GenerationStats(0, 4.0, 4.0, 4.0, 0.0, 1), List.of(new Evaluated(only, 4.0)));

      StoredAgent agent = store.loadAgent(1L).orElseThrow();
      assertThat(agent.fitness()).isEqualTo(4.0);
      assertThat(agent.genome()).isEqualTo(only);
    }
  }

  @Test
  void missingRunYieldsEmpty() {
    try (RunStore store = open()) {
      assertThat(store.loadRun(99L)).isEmpty();
      assertThat(store.loadGenerations(99L)).isEmpty();
      assertThat(store.loadOverallChampion(99L)).isEmpty();
      assertThat(store.loadAgent(99L)).isEmpty();
      assertThat(store.listRuns()).isEmpty();
    }
  }

  private RunStore open() {
    return new RunStore("jdbc:sqlite:" + dir.resolve("runs.db"), new ObjectMapper());
  }
}
