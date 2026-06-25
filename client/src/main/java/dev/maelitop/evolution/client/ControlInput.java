package dev.maelitop.evolution.client;

import dev.maelitop.evolution.protocol.ClientMessage;
import dev.maelitop.evolution.protocol.Pause;
import dev.maelitop.evolution.protocol.Resume;
import dev.maelitop.evolution.protocol.SetSpeed;
import dev.maelitop.evolution.protocol.Step;
import java.util.Optional;

final class ControlInput {

  enum Command {
    TOGGLE_PAUSE,
    STEP,
    FASTER,
    SLOWER
  }

  private static final double MIN_SPEED = 0.25;
  private static final double MAX_SPEED = 8.0;

  private boolean paused;
  private double speed = 1.0;

  Optional<ClientMessage> on(Command command) {
    return switch (command) {
      case TOGGLE_PAUSE -> {
        paused = !paused;
        yield Optional.of(paused ? new Pause() : new Resume());
      }
      case STEP -> Optional.of(new Step());
      case FASTER -> scale(2.0);
      case SLOWER -> scale(0.5);
    };
  }

  boolean paused() {
    return paused;
  }

  double speed() {
    return speed;
  }

  private Optional<ClientMessage> scale(double factor) {
    double next = Math.clamp(speed * factor, MIN_SPEED, MAX_SPEED);
    if (next == speed) {
      return Optional.empty();
    }
    speed = next;
    return Optional.of(new SetSpeed(speed));
  }
}
