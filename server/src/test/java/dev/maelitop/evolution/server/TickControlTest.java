package dev.maelitop.evolution.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TickControlTest {

  @Test
  void allowsEveryTickWhileRunning() {
    TickControl control = new TickControl();

    assertThat(control.allow()).isTrue();
    assertThat(control.allow()).isTrue();
  }

  @Test
  void blocksTicksWhilePaused() {
    TickControl control = new TickControl();
    control.pause();

    assertThat(control.allow()).isFalse();
  }

  @Test
  void stepReleasesExactlyOneTickWhilePaused() {
    TickControl control = new TickControl();
    control.pause();
    control.requestStep();

    assertThat(control.allow()).isTrue();
    assertThat(control.allow()).isFalse();
  }

  @Test
  void stepsQueueWhilePaused() {
    TickControl control = new TickControl();
    control.pause();
    control.requestStep();
    control.requestStep();

    assertThat(control.allow()).isTrue();
    assertThat(control.allow()).isTrue();
    assertThat(control.allow()).isFalse();
  }

  @Test
  void resumeDiscardsQueuedSteps() {
    TickControl control = new TickControl();
    control.pause();
    control.requestStep();
    control.resume();
    control.pause();

    assertThat(control.allow()).isFalse();
  }
}
