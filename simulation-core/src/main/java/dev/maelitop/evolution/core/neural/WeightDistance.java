package dev.maelitop.evolution.core.neural;

import java.util.List;

public final class WeightDistance {

  private WeightDistance() {}

  public static double euclidean(Genome a, Genome b) {
    List<ConnectionGene> first = a.connections();
    List<ConnectionGene> second = b.connections();
    if (first.size() != second.size()) {
      throw new IllegalArgumentException(
          "genomes have divergent structure: " + first.size() + " vs " + second.size());
    }
    double sum = 0.0;
    for (int i = 0; i < first.size(); i++) {
      double d = first.get(i).weight() - second.get(i).weight();
      sum += d * d;
    }
    return Math.sqrt(sum);
  }
}
