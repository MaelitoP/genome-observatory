package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.neural.GenomeDistance;
import java.util.List;

public interface EvolutionStrategy {

  List<Genome> evolve(List<Evaluated> population, int targetSize);

  GenomeDistance distance();
}
