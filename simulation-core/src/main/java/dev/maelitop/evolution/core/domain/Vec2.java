package dev.maelitop.evolution.core.domain;

public record Vec2(double x, double y) {

  public Vec2 add(Vec2 other) {
    return new Vec2(x + other.x, y + other.y);
  }

  public Vec2 scale(double factor) {
    return new Vec2(x * factor, y * factor);
  }

  public double distanceTo(Vec2 other) {
    return Math.hypot(x - other.x, y - other.y);
  }
}
