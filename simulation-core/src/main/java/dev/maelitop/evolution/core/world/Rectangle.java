package dev.maelitop.evolution.core.world;

public record Rectangle(double x, double y, double width, double height) {

  public Rectangle {
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("width/height must be non-negative");
    }
  }

  public boolean contains(double px, double py) {
    return px >= x && px < x + width && py >= y && py < y + height;
  }

  public boolean intersects(Rectangle other) {
    return x < other.x + other.width
        && x + width > other.x
        && y < other.y + other.height
        && y + height > other.y;
  }
}
