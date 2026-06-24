package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.neural.GenomeDistance;
import java.util.List;

public final class Diversity {

  private Diversity() {}

  public static double meanPairwiseDistance(List<Genome> genomes, GenomeDistance metric) {
    int n = genomes.size();
    if (n < 2) {
      return 0.0;
    }
    double total = 0.0;
    for (int i = 0; i < n; i++) {
      Genome a = genomes.get(i);
      for (int j = i + 1; j < n; j++) {
        total += metric.between(a, genomes.get(j));
      }
    }
    return total / (n * (n - 1) / 2.0);
  }
}
