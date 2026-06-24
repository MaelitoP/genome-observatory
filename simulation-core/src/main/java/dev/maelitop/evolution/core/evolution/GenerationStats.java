package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.neural.GenomeDistance;
import java.util.List;

public record GenerationStats(
    int generation,
    double bestFitness,
    double meanFitness,
    double medianFitness,
    double diversity,
    int population) {

  public static GenerationStats summarize(
      int generation, List<Evaluated> evaluated, GenomeDistance metric) {
    int n = evaluated.size();
    if (n == 0) {
      throw new IllegalArgumentException("cannot summarize an empty population");
    }
    double best = Double.NEGATIVE_INFINITY;
    double sum = 0.0;
    for (Evaluated entry : evaluated) {
      best = Math.max(best, entry.fitness());
      sum += entry.fitness();
    }
    double[] sorted = evaluated.stream().mapToDouble(Evaluated::fitness).sorted().toArray();
    int mid = n / 2;
    double median = n % 2 == 0 ? (sorted[mid - 1] + sorted[mid]) / 2.0 : sorted[mid];
    double diversity =
        Diversity.meanPairwiseDistance(evaluated.stream().map(Evaluated::genome).toList(), metric);
    return new GenerationStats(generation, best, sum / n, median, diversity, n);
  }
}
