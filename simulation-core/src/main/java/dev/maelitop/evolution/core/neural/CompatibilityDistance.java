package dev.maelitop.evolution.core.neural;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CompatibilityDistance implements GenomeDistance {

  private final double excessCoefficient;
  private final double disjointCoefficient;
  private final double weightCoefficient;

  public CompatibilityDistance(
      double excessCoefficient, double disjointCoefficient, double weightCoefficient) {
    this.excessCoefficient = excessCoefficient;
    this.disjointCoefficient = disjointCoefficient;
    this.weightCoefficient = weightCoefficient;
  }

  @Override
  public double between(Genome a, Genome b) {
    Map<Integer, Double> weightsA = weightsByInnovation(a);
    Map<Integer, Double> weightsB = weightsByInnovation(b);
    int boundary = Math.min(maxInnovation(a), maxInnovation(b));

    Set<Integer> innovations = new HashSet<>(weightsA.keySet());
    innovations.addAll(weightsB.keySet());

    int excess = 0;
    int disjoint = 0;
    int matching = 0;
    double weightDifference = 0.0;
    for (int innovation : innovations) {
      Double inA = weightsA.get(innovation);
      Double inB = weightsB.get(innovation);
      if (inA != null && inB != null) {
        matching++;
        weightDifference += Math.abs(inA - inB);
      } else if (innovation > boundary) {
        excess++;
      } else {
        disjoint++;
      }
    }

    int normalizer = Math.max(1, Math.max(weightsA.size(), weightsB.size()));
    double meanWeightDifference = matching == 0 ? 0.0 : weightDifference / matching;
    return excessCoefficient * excess / normalizer
        + disjointCoefficient * disjoint / normalizer
        + weightCoefficient * meanWeightDifference;
  }

  private static Map<Integer, Double> weightsByInnovation(Genome genome) {
    Map<Integer, Double> weights = new HashMap<>(genome.connections().size() * 2);
    for (ConnectionGene gene : genome.connections()) {
      weights.put(gene.innovation(), gene.weight());
    }
    return weights;
  }

  private static int maxInnovation(Genome genome) {
    return genome.connections().stream().mapToInt(ConnectionGene::innovation).max().orElse(-1);
  }
}
