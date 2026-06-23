package dev.maelitop.evolution.protocol;

public record SetSpeed(double multiplier) implements ClientMessage {

  public SetSpeed {
    if (multiplier <= 0) {
      throw new IllegalArgumentException("multiplier must be positive: " + multiplier);
    }
  }
}
