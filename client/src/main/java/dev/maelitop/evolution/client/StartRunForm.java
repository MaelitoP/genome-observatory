package dev.maelitop.evolution.client;

final class StartRunForm {

  enum Field {
    SEED,
    GENERATIONS,
    CARNIVORES
  }

  static final int MAX_GENERATIONS = 1000;
  static final int MAX_CARNIVORES = 100;
  private static final int MAX_DIGITS = 12;

  private boolean open;
  private Field field = Field.SEED;
  private final StringBuilder seed = new StringBuilder("42");
  private final StringBuilder generations = new StringBuilder("100");
  private final StringBuilder carnivores = new StringBuilder("0");

  boolean open() {
    return open;
  }

  void toggle() {
    open = !open;
  }

  void close() {
    open = false;
  }

  Field field() {
    return field;
  }

  void nextField() {
    field =
        switch (field) {
          case SEED -> Field.GENERATIONS;
          case GENERATIONS -> Field.CARNIVORES;
          case CARNIVORES -> Field.SEED;
        };
  }

  void prevField() {
    field =
        switch (field) {
          case SEED -> Field.CARNIVORES;
          case GENERATIONS -> Field.SEED;
          case CARNIVORES -> Field.GENERATIONS;
        };
  }

  void typeDigit(int digit) {
    StringBuilder buffer = buffer(field);
    if (buffer.length() < MAX_DIGITS) {
      buffer.append((char) ('0' + digit));
    }
  }

  void backspace() {
    StringBuilder buffer = buffer(field);
    if (!buffer.isEmpty()) {
      buffer.deleteCharAt(buffer.length() - 1);
    }
  }

  String text(Field f) {
    return buffer(f).toString();
  }

  long seed() {
    return parse(seed, 0);
  }

  int generations() {
    return (int) Math.clamp(parse(generations, 1), 1, MAX_GENERATIONS);
  }

  int carnivores() {
    return (int) Math.clamp(parse(carnivores, 0), 0, MAX_CARNIVORES);
  }

  private StringBuilder buffer(Field f) {
    return switch (f) {
      case SEED -> seed;
      case GENERATIONS -> generations;
      case CARNIVORES -> carnivores;
    };
  }

  private static long parse(StringBuilder buffer, long fallback) {
    if (buffer.isEmpty()) {
      return fallback;
    }
    try {
      return Long.parseLong(buffer.toString());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
