package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.neural.ConnectionGene;
import dev.maelitop.evolution.core.neural.Genome;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public final class Mutation {

  private final RandomGenerator rng;

  public Mutation(RandomGenerator rng) {
    this.rng = rng;
  }

  public Genome mutateWeights(Genome genome, double rate, double sigma) {
    List<ConnectionGene> mutated = new ArrayList<>(genome.connections().size());
    for (ConnectionGene gene : genome.connections()) {
      if (rng.nextDouble() < rate) {
        mutated.add(gene.withWeight(gene.weight() + rng.nextGaussian() * sigma));
      } else {
        mutated.add(gene);
      }
    }
    return new Genome(genome.nodes(), mutated);
  }
}
