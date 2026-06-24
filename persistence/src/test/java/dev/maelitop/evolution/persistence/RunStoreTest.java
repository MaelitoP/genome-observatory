package dev.maelitop.evolution.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.Evaluated;
import dev.maelitop.evolution.core.evolution.GenerationStats;
import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.Genome;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
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
      runId = store.startRun(new RunSpec(42L, WorldConfig.defaults(), 2, 0, 1000L));
      store.recordGeneration(
          runId,
          Team.HERBIVORE,
          new GenerationStats(0, 5.0, 3.0, 3.0, 0.5, 2),
          List.of(new Evaluated(g0, 1.0), new Evaluated(champion, 5.0)));
      store.recordGeneration(
          runId,
          Team.HERBIVORE,
          new GenerationStats(1, 3.0, 2.5, 2.5, 0.4, 1),
          List.of(new Evaluated(g2, 3.0)));
    }

    try (RunStore store = open()) {
      assertThat(store.loadRun(runId))
          .contains(new StoredRun(runId, 42L, WorldConfig.defaults(), 2, 0, 1000L));
      assertThat(store.loadGenerations(runId, Team.HERBIVORE))
          .containsExactly(
              new GenerationStats(0, 5.0, 3.0, 3.0, 0.5, 2),
              new GenerationStats(1, 3.0, 2.5, 2.5, 0.4, 1));
      assertThat(store.loadOverallChampion(runId)).contains(champion);
    }
  }

  @Test
  void storesAndLoadsBothTeamsPerGeneration() {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(5L);
    Genome herbivore = Genome.initial(14, OUTPUTS, rng);
    Genome carnivore = Genome.initial(14, OUTPUTS, rng);
    GenerationStats herbivoreStats = new GenerationStats(0, 5.0, 4.0, 4.0, 0.1, 6);
    GenerationStats carnivoreStats = new GenerationStats(0, 3.0, 2.0, 2.0, 0.2, 4);

    long runId;
    try (RunStore store = open()) {
      runId = store.startRun(new RunSpec(11L, WorldConfig.defaults(), 1, 4, 0L));
      store.recordGeneration(
          runId, Team.HERBIVORE, herbivoreStats, List.of(new Evaluated(herbivore, 5.0)));
      store.recordGeneration(
          runId, Team.CARNIVORE, carnivoreStats, List.of(new Evaluated(carnivore, 3.0)));
    }

    try (RunStore store = open()) {
      assertThat(store.loadRun(runId).orElseThrow().carnivores()).isEqualTo(4);
      assertThat(store.loadGenerations(runId, Team.HERBIVORE)).containsExactly(herbivoreStats);
      assertThat(store.loadGenerations(runId, Team.CARNIVORE)).containsExactly(carnivoreStats);
      assertThat(store.loadGenerationRecords(runId))
          .containsExactlyInAnyOrder(
              new GenerationRecord(Team.HERBIVORE, herbivoreStats),
              new GenerationRecord(Team.CARNIVORE, carnivoreStats));
      assertThat(store.loadChampion(runId, Team.HERBIVORE)).contains(herbivore);
      assertThat(store.loadChampion(runId, Team.CARNIVORE)).contains(carnivore);
    }
  }

  @Test
  void migratesLegacyDatabaseInPlace() throws Exception {
    String url = "jdbc:sqlite:" + dir.resolve("legacy.db");
    String configJson = new ObjectMapper().writeValueAsString(WorldConfig.defaults());
    try (Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "CREATE TABLE run (id INTEGER PRIMARY KEY AUTOINCREMENT, seed INTEGER NOT NULL,"
              + " config TEXT NOT NULL, generations INTEGER NOT NULL, started_at INTEGER NOT NULL)");
      statement.executeUpdate(
          "CREATE TABLE generation (id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + " run_id INTEGER NOT NULL, idx INTEGER NOT NULL, best_fitness REAL NOT NULL,"
              + " mean_fitness REAL NOT NULL, median_fitness REAL NOT NULL, diversity REAL NOT NULL,"
              + " population INTEGER NOT NULL)");
      statement.executeUpdate(
          "CREATE TABLE agent (id INTEGER PRIMARY KEY AUTOINCREMENT, generation_id INTEGER NOT NULL,"
              + " genome_json TEXT NOT NULL, fitness REAL NOT NULL)");
      try (PreparedStatement insert =
          connection.prepareStatement(
              "INSERT INTO run(seed, config, generations, started_at) VALUES (?,?,?,?)")) {
        insert.setLong(1, 5L);
        insert.setString(2, configJson);
        insert.setInt(3, 1);
        insert.setLong(4, 123L);
        insert.executeUpdate();
      }
    }

    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(9L);
    try (RunStore store = new RunStore(url, new ObjectMapper())) {
      assertThat(store.loadRun(1L).orElseThrow().carnivores()).isZero();
      store.recordGeneration(
          1L,
          Team.HERBIVORE,
          new GenerationStats(0, 1.0, 1.0, 1.0, 0.0, 1),
          List.of(new Evaluated(Genome.initial(14, OUTPUTS, rng), 1.0)));
      assertThat(store.loadGenerations(1L, Team.HERBIVORE)).hasSize(1);
    }
  }

  @Test
  void championTieBreakIsDeterministic() {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(2L);
    Genome first = Genome.initial(14, OUTPUTS, rng);
    Genome second = Genome.initial(14, OUTPUTS, rng);

    long runId;
    try (RunStore store = open()) {
      runId = store.startRun(new RunSpec(7L, WorldConfig.defaults(), 1, 0, 0L));
      store.recordGeneration(
          runId,
          Team.HERBIVORE,
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
                      Team.HERBIVORE,
                      new GenerationStats(0, 1.0, 1.0, 1.0, 0.0, 1),
                      List.of(new Evaluated(Genome.initial(14, OUTPUTS, rng), 1.0))))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("failed to record generation");
    }
  }

  @Test
  void listsRunsInInsertionOrder() {
    try (RunStore store = open()) {
      long first = store.startRun(new RunSpec(1L, WorldConfig.defaults(), 3, 0, 100L));
      long second = store.startRun(new RunSpec(2L, WorldConfig.defaults(), 5, 8, 200L));

      assertThat(store.listRuns())
          .containsExactly(
              new StoredRun(first, 1L, WorldConfig.defaults(), 3, 0, 100L),
              new StoredRun(second, 2L, WorldConfig.defaults(), 5, 8, 200L));
    }
  }

  @Test
  void loadsAgentByIdWithGenomeAndTeam() {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(4L);
    Genome only = Genome.initial(14, OUTPUTS, rng);

    try (RunStore store = open()) {
      long runId = store.startRun(new RunSpec(8L, WorldConfig.defaults(), 1, 0, 0L));
      store.recordGeneration(
          runId,
          Team.HERBIVORE,
          new GenerationStats(0, 4.0, 4.0, 4.0, 0.0, 1),
          List.of(new Evaluated(only, 4.0)));

      StoredAgent agent = store.loadAgent(1L).orElseThrow();
      assertThat(agent.team()).isEqualTo(Team.HERBIVORE);
      assertThat(agent.fitness()).isEqualTo(4.0);
      assertThat(agent.genome()).isEqualTo(only);
    }
  }

  @Test
  void missingRunYieldsEmpty() {
    try (RunStore store = open()) {
      assertThat(store.loadRun(99L)).isEmpty();
      assertThat(store.loadGenerations(99L, Team.HERBIVORE)).isEmpty();
      assertThat(store.loadGenerationRecords(99L)).isEmpty();
      assertThat(store.loadChampion(99L, Team.HERBIVORE)).isEmpty();
      assertThat(store.loadOverallChampion(99L)).isEmpty();
      assertThat(store.loadAgent(99L)).isEmpty();
      assertThat(store.listRuns()).isEmpty();
    }
  }

  private RunStore open() {
    return new RunStore("jdbc:sqlite:" + dir.resolve("runs.db"), new ObjectMapper());
  }
}
