package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.neural.ConnectionGene;
import dev.maelitop.evolution.core.neural.Genome;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

public final class Crossover {

  private final RandomGenerator rng;

  public Crossover(RandomGenerator rng) {
    this.rng = rng;
  }

  public Genome cross(Genome a, Genome b) {
    Map<Integer, ConnectionGene> fromB = new HashMap<>(b.connections().size() * 2);
    for (ConnectionGene gene : b.connections()) {
      fromB.put(gene.innovation(), gene);
    }
    List<ConnectionGene> child = new ArrayList<>(a.connections().size());
    for (ConnectionGene gene : a.connections()) {
      ConnectionGene match = fromB.get(gene.innovation());
      child.add(match != null && rng.nextBoolean() ? match : gene);
    }
    return new Genome(a.nodes(), child);
  }
}
