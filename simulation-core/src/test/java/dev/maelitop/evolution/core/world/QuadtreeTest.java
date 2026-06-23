package dev.maelitop.evolution.core.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class QuadtreeTest {

  private static final Rectangle WORLD = new Rectangle(0, 0, 100, 100);

  @Test
  void returnsPointsInsideRegion() {
    Quadtree<String> tree = Quadtree.create(WORLD);
    tree.insert(10, 10, "a");
    tree.insert(50, 50, "b");
    tree.insert(90, 90, "c");

    assertThat(tree.query(new Rectangle(0, 0, 60, 60))).containsExactlyInAnyOrder("a", "b");
  }

  @Test
  void excludesPointsOutsideRegion() {
    Quadtree<String> tree = Quadtree.create(WORLD);
    tree.insert(10, 10, "a");

    assertThat(tree.query(new Rectangle(40, 40, 20, 20))).isEmpty();
  }

  @Test
  void queryDisjointFromBoundsIsEmpty() {
    Quadtree<String> tree = Quadtree.create(WORLD);
    tree.insert(10, 10, "a");

    assertThat(tree.query(new Rectangle(200, 200, 10, 10))).isEmpty();
  }

  @Test
  void rejectsPointOutsideBounds() {
    Quadtree<String> tree = Quadtree.create(WORLD);

    assertThatThrownBy(() -> tree.insert(100, 100, "edge"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvalidConstruction() {
    assertThatThrownBy(() -> new Quadtree<>(null, 8, 8)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Quadtree<>(WORLD, 0, 8))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Quadtree<>(WORLD, 8, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void returnsEmptyForEmptyTree() {
    assertThat(Quadtree.<String>create(WORLD).query(WORLD)).isEmpty();
  }

  @Test
  void queryStraddlingAllFourQuadrantsFindsEntitiesInEach() {
    Quadtree<String> tree = new Quadtree<>(WORLD, 1, 8);
    tree.insert(45, 45, "nw");
    tree.insert(55, 45, "ne");
    tree.insert(45, 55, "sw");
    tree.insert(55, 55, "se");

    assertThat(tree.query(new Rectangle(40, 40, 20, 20)))
        .containsExactlyInAnyOrder("nw", "ne", "sw", "se");
  }

  @Test
  void queryWithinOneQuadrantIgnoresOthers() {
    Quadtree<String> tree = new Quadtree<>(WORLD, 1, 8);
    tree.insert(25, 25, "nw");
    tree.insert(75, 25, "ne");
    tree.insert(25, 75, "sw");
    tree.insert(75, 75, "se");

    assertThat(tree.query(new Rectangle(0, 0, 40, 40))).containsExactly("nw");
  }

  @Test
  void findsPointOnQuadrantSeamExactlyOnce() {
    Quadtree<String> tree = new Quadtree<>(WORLD, 1, 8);
    tree.insert(50, 50, "center");
    tree.insert(10, 10, "filler");

    assertThat(tree.query(WORLD)).containsExactlyInAnyOrder("center", "filler");
  }

  @Test
  void isReusableAfterClear() {
    Quadtree<String> tree = new Quadtree<>(WORLD, 1, 8);
    tree.insert(10, 10, "a");
    tree.insert(90, 90, "b");

    tree.clear();
    assertThat(tree.query(WORLD)).isEmpty();

    tree.insert(50, 50, "c");
    assertThat(tree.query(WORLD)).containsExactly("c");
  }

  @Test
  void matchesBruteForceUnderDeepSubdivision() {
    Random random = new Random(42);
    Quadtree<Integer> tree = new Quadtree<>(WORLD, 2, 12);
    List<double[]> points = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      double x = random.nextDouble() * 100;
      double y = random.nextDouble() * 100;
      points.add(new double[] {x, y});
      tree.insert(x, y, i);
    }

    for (int q = 0; q < 200; q++) {
      Rectangle region = randomRegion(random);
      List<Integer> expected = new ArrayList<>();
      for (int i = 0; i < points.size(); i++) {
        double[] p = points.get(i);
        if (region.contains(p[0], p[1])) {
          expected.add(i);
        }
      }
      assertThat(tree.query(region)).containsExactlyInAnyOrderElementsOf(expected);
    }
  }

  private static Rectangle randomRegion(Random random) {
    double x = random.nextDouble() * 100;
    double y = random.nextDouble() * 100;
    double w = random.nextDouble() * (100 - x);
    double h = random.nextDouble() * (100 - y);
    return new Rectangle(x, y, w, h);
  }
}
