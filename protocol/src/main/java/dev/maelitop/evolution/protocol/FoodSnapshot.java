package dev.maelitop.evolution.protocol;

public record FoodSnapshot(long id, double x, double y) implements EntitySnapshot {
  @Override
  public EntityKind kind() {
    return EntityKind.FOOD;
  }
}
