package dev.maelitop.evolution.client;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.client.ControlInput.Command;
import dev.maelitop.evolution.protocol.Pause;
import dev.maelitop.evolution.protocol.Resume;
import dev.maelitop.evolution.protocol.SetSpeed;
import dev.maelitop.evolution.protocol.Step;
import org.junit.jupiter.api.Test;

class ControlInputTest {

  @Test
  void togglePauseAlternatesBetweenPauseAndResume() {
    ControlInput control = new ControlInput();

    assertThat(control.on(Command.TOGGLE_PAUSE)).containsInstanceOf(Pause.class);
    assertThat(control.paused()).isTrue();
    assertThat(control.on(Command.TOGGLE_PAUSE)).containsInstanceOf(Resume.class);
    assertThat(control.paused()).isFalse();
  }

  @Test
  void stepAlwaysEmitsStep() {
    ControlInput control = new ControlInput();

    assertThat(control.on(Command.STEP)).containsInstanceOf(Step.class);
    assertThat(control.on(Command.STEP)).containsInstanceOf(Step.class);
  }

  @Test
  void fasterDoublesSpeedUpToCap() {
    ControlInput control = new ControlInput();

    assertThat(control.on(Command.FASTER)).contains(new SetSpeed(2.0));
    assertThat(control.on(Command.FASTER)).contains(new SetSpeed(4.0));
    assertThat(control.on(Command.FASTER)).contains(new SetSpeed(8.0));
    assertThat(control.on(Command.FASTER)).isEmpty();
  }

  @Test
  void slowerHalvesSpeedDownToFloor() {
    ControlInput control = new ControlInput();

    assertThat(control.on(Command.SLOWER)).contains(new SetSpeed(0.5));
    assertThat(control.on(Command.SLOWER)).contains(new SetSpeed(0.25));
    assertThat(control.on(Command.SLOWER)).isEmpty();
  }

  @Test
  void slowerReversesAfterReachingCap() {
    ControlInput control = new ControlInput();
    control.on(Command.FASTER);
    control.on(Command.FASTER);
    control.on(Command.FASTER);

    assertThat(control.on(Command.SLOWER)).contains(new SetSpeed(4.0));
  }
}
