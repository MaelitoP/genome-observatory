package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.neural.ConnectionGene;
import dev.maelitop.evolution.core.neural.Genome;
import java.util.List;

public final class Diversity {

  private Diversity() {}

  public static double meanPairwiseDistance(List<Genome> genomes) {
    int n = genomes.size();
    if (n < 2) {
      return 0.0;
    }
    double total = 0.0;
    for (int i = 0; i < n; i++) {
      List<ConnectionGene> a = genomes.get(i).connections();
      for (int j = i + 1; j < n; j++) {
        total += distance(a, genomes.get(j).connections());
      }
    }
    return total / (n * (n - 1) / 2.0);
  }

  private static double distance(List<ConnectionGene> a, List<ConnectionGene> b) {
    if (a.size() != b.size()) {
      throw new IllegalArgumentException(
          "genomes have divergent structure: " + a.size() + " vs " + b.size());
    }
    double sum = 0.0;
    for (int k = 0; k < a.size(); k++) {
      double d = a.get(k).weight() - b.get(k).weight();
      sum += d * d;
    }
    return Math.sqrt(sum);
  }
}
