package dev.maelitop.evolution.core.neural;

public record ConnectionGene(int in, int out, double weight, boolean enabled, int innovation) {

  public ConnectionGene withWeight(double newWeight) {
    return new ConnectionGene(in, out, newWeight, enabled, innovation);
  }
}
