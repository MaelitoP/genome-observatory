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

  public Genome cross(Genome fitter, Genome other) {
    Map<Integer, ConnectionGene> otherByInnovation = new HashMap<>(other.connections().size() * 2);
    for (ConnectionGene gene : other.connections()) {
      otherByInnovation.put(gene.innovation(), gene);
    }
    List<ConnectionGene> child = new ArrayList<>(fitter.connections().size());
    for (ConnectionGene gene : fitter.connections()) {
      ConnectionGene match = otherByInnovation.get(gene.innovation());
      child.add(match != null && rng.nextBoolean() ? match : gene);
    }
    return new Genome(fitter.nodes(), child);
  }
}
