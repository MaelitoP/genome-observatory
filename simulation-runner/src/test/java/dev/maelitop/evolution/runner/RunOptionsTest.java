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
}
