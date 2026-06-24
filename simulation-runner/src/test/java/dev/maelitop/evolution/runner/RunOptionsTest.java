package dev.maelitop.evolution.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RunOptionsTest {

  @Test
  void defaultsWhenNoArguments() {
    RunOptions options = RunOptions.parse(new String[] {});

    assertThat(options.seed()).isZero();
    assertThat(options.generations()).isEqualTo(100);
  }

  @Test
  void parsesSeedAndGenerations() {
    RunOptions options = RunOptions.parse(new String[] {"--seed", "42", "--generations", "5"});

    assertThat(options.seed()).isEqualTo(42L);
    assertThat(options.generations()).isEqualTo(5);
  }

  @Test
  void rejectsUnknownFlag() {
    assertThatThrownBy(() -> RunOptions.parse(new String[] {"--bogus"}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown argument");
  }

  @Test
  void rejectsMissingValue() {
    assertThatThrownBy(() -> RunOptions.parse(new String[] {"--seed"}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing value");
  }

  @Test
  void rejectsNonPositiveGenerations() {
    assertThatThrownBy(() -> RunOptions.parse(new String[] {"--generations", "0"}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("generations must be positive");
  }

  @Test
  void parsesPersistenceFlags() {
    RunOptions options =
        RunOptions.parse(new String[] {"--db", "runs.db", "--export-champion", "champion.json"});

    assertThat(options.dbPath()).isEqualTo("runs.db");
    assertThat(options.exportChampionPath()).isEqualTo("champion.json");
    assertThat(options.replayRunId()).isNull();
  }

  @Test
  void parsesReplayWithDb() {
    RunOptions options = RunOptions.parse(new String[] {"--db", "runs.db", "--replay", "7"});

    assertThat(options.replayRunId()).isEqualTo(7L);
  }

  @Test
  void rejectsReplayWithoutDb() {
    assertThatThrownBy(() -> RunOptions.parse(new String[] {"--replay", "1"}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--replay requires --db");
  }

  @Test
  void rejectsExportWithoutDb() {
    assertThatThrownBy(() -> RunOptions.parse(new String[] {"--export-champion", "c.json"}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("--export-champion requires --db");
  }

  @Test
  void defaultsToWeightsStrategyWithoutCarnivores() {
    RunOptions options = RunOptions.parse(new String[] {});

    assertThat(options.strategy()).isEqualTo(Strategy.WEIGHTS);
    assertThat(options.coEvolution()).isFalse();
  }

  @Test
  void parsesNeatStrategyAndCarnivores() {
    RunOptions options =
        RunOptions.parse(new String[] {"--strategy", "neat", "--carnivores", "10"});

    assertThat(options.strategy()).isEqualTo(Strategy.NEAT);
    assertThat(options.carnivores()).isEqualTo(10);
    assertThat(options.coEvolution()).isTrue();
  }

  @Test
  void rejectsUnknownStrategy() {
    assertThatThrownBy(() -> RunOptions.parse(new String[] {"--strategy", "magic"}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown strategy");
  }

  @Test
  void allowsCoEvolutionWithDb() {
    RunOptions options = RunOptions.parse(new String[] {"--carnivores", "5", "--db", "runs.db"});

    assertThat(options.carnivores()).isEqualTo(5);
    assertThat(options.dbPath()).isEqualTo("runs.db");
    assertThat(options.coEvolution()).isTrue();
  }
}
