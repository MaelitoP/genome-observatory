package dev.maelitop.evolution.protocol;

public record HelloMessage(
    String runId, long seed, WorldConfig worldConfig, long startedAtEpochMs, int tickRate)
    implements ServerMessage {

  public HelloMessage {
    if (tickRate <= 0) {
      throw new IllegalArgumentException("tickRate must be positive: " + tickRate);
    }
  }
}
