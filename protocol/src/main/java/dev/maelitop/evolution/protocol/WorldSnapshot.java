package dev.maelitop.evolution.protocol;

import java.util.List;

public record WorldSnapshot(
    long tick, int generation, int population, WorldStats stats, List<EntitySnapshot> entities)
    implements ServerMessage {

  public WorldSnapshot {
    entities = List.copyOf(entities);
  }
}
