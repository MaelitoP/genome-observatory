package dev.maelitop.evolution.core.neural;

public enum Activation {
  IDENTITY,
  TANH,
  SIGMOID;

  public double apply(double x) {
    return switch (this) {
      case IDENTITY -> x;
      case TANH -> Math.tanh(x);
      case SIGMOID -> 1.0 / (1.0 + Math.exp(-x));
    };
  }
}
