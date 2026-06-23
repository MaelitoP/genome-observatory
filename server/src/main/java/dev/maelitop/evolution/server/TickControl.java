package dev.maelitop.evolution.server;

import java.util.concurrent.atomic.AtomicInteger;

/** Pause/resume/step gate for the ticker; {@link #allow()} is called only by the ticker thread. */
final class TickControl {

  private final AtomicInteger pendingSteps = new AtomicInteger();
  private volatile boolean paused;

  void pause() {
    paused = true;
  }

  void resume() {
    pendingSteps.set(0);
    paused = false;
  }

  void requestStep() {
    pendingSteps.incrementAndGet();
  }

  boolean allow() {
    if (!paused) {
      return true;
    }
    if (pendingSteps.get() <= 0) {
      return false;
    }
    pendingSteps.decrementAndGet();
    return true;
  }
}
