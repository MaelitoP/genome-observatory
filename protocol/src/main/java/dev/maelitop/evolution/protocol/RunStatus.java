package dev.maelitop.evolution.protocol;

/** Lifecycle state of a persisted run, shared across persistence, server and client. */
public enum RunStatus {
  QUEUED,
  RUNNING,
  COMPLETED,
  INTERRUPTED;

  public boolean terminal() {
    return this == COMPLETED || this == INTERRUPTED;
  }

  /** Tolerant parse of a stored value: null or an unrecognized string maps to INTERRUPTED. */
  public static RunStatus fromDb(String value) {
    if (value == null) {
      return INTERRUPTED;
    }
    try {
      return valueOf(value);
    } catch (IllegalArgumentException e) {
      return INTERRUPTED;
    }
  }
}
