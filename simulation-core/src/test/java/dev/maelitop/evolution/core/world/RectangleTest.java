package dev.maelitop.evolution.core.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RectangleTest {

  @Test
  void containsIsHalfOpen() {
    Rectangle rect = new Rectangle(0, 0, 10, 10);

    assertThat(rect.contains(0, 0)).isTrue();
    assertThat(rect.contains(9.999, 9.999)).isTrue();
    assertThat(rect.contains(10, 5)).isFalse();
    assertThat(rect.contains(5, 10)).isFalse();
  }

  @Test
  void intersectsDetectsOverlapButNotTouchingEdges() {
    Rectangle rect = new Rectangle(0, 0, 10, 10);

    assertThat(rect.intersects(new Rectangle(5, 5, 10, 10))).isTrue();
    assertThat(rect.intersects(new Rectangle(10, 0, 10, 10))).isFalse();
    assertThat(rect.intersects(new Rectangle(20, 20, 5, 5))).isFalse();
  }

  @Test
  void rejectsNegativeExtents() {
    assertThatThrownBy(() -> new Rectangle(0, 0, -1, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
