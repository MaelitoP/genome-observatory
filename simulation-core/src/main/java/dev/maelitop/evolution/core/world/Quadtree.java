package dev.maelitop.evolution.core.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Quadtree<T> {

  private static final int DEFAULT_CAPACITY = 8;
  private static final int DEFAULT_MAX_DEPTH = 8;

  private record Point<T>(double x, double y, T value) {}

  private final Rectangle bounds;
  private final int capacity;
  private final int maxDepth;
  private final int depth;
  private final List<Point<T>> points = new ArrayList<>();
  private List<Quadtree<T>> children;

  public Quadtree(Rectangle bounds, int capacity, int maxDepth) {
    Objects.requireNonNull(bounds, "bounds");
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    if (maxDepth < 0) {
      throw new IllegalArgumentException("maxDepth must be non-negative");
    }
    this(bounds, capacity, maxDepth, 0);
  }

  private Quadtree(Rectangle bounds, int capacity, int maxDepth, int depth) {
    this.bounds = bounds;
    this.capacity = capacity;
    this.maxDepth = maxDepth;
    this.depth = depth;
  }

  public static <T> Quadtree<T> create(Rectangle bounds) {
    return new Quadtree<>(bounds, DEFAULT_CAPACITY, DEFAULT_MAX_DEPTH);
  }

  public void insert(double x, double y, T value) {
    if (!bounds.contains(x, y)) {
      throw new IllegalArgumentException("point (" + x + ", " + y + ") is outside " + bounds);
    }
    insert(new Point<>(x, y, value));
  }

  private void insert(Point<T> point) {
    if (children != null) {
      childContaining(point).insert(point);
      return;
    }
    points.add(point);
    if (points.size() > capacity && depth < maxDepth) {
      subdivide();
    }
  }

  private void subdivide() {
    double halfW = bounds.width() / 2;
    double halfH = bounds.height() / 2;
    double x = bounds.x();
    double y = bounds.y();
    children =
        List.of(
            newChild(new Rectangle(x, y, halfW, halfH)),
            newChild(new Rectangle(x + halfW, y, bounds.width() - halfW, halfH)),
            newChild(new Rectangle(x, y + halfH, halfW, bounds.height() - halfH)),
            newChild(
                new Rectangle(
                    x + halfW, y + halfH, bounds.width() - halfW, bounds.height() - halfH)));
    for (Point<T> point : points) {
      childContaining(point).insert(point);
    }
    points.clear();
  }

  private Quadtree<T> newChild(Rectangle childBounds) {
    return new Quadtree<>(childBounds, capacity, maxDepth, depth + 1);
  }

  private Quadtree<T> childContaining(Point<T> point) {
    for (Quadtree<T> child : children) {
      if (child.bounds.contains(point.x(), point.y())) {
        return child;
      }
    }
    throw new IllegalStateException("in-bounds point fits no child quadrant");
  }

  public List<T> query(Rectangle region) {
    List<T> hits = new ArrayList<>();
    query(region, hits);
    return hits;
  }

  private void query(Rectangle region, List<T> hits) {
    if (!bounds.intersects(region)) {
      return;
    }
    for (Point<T> point : points) {
      if (region.contains(point.x(), point.y())) {
        hits.add(point.value());
      }
    }
    if (children != null) {
      for (Quadtree<T> child : children) {
        child.query(region, hits);
      }
    }
  }

  public void clear() {
    points.clear();
    children = null;
  }
}
