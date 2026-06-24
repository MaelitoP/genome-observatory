package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.neural.GenomeDistance;
import dev.maelitop.evolution.core.neural.WeightDistance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

public final class WeightsOnlyStrategy implements EvolutionStrategy {

  private static final int TOURNAMENT_SIZE = 3;
  private static final double ELITE_FRACTION = 0.1;
  private static final double MUTATION_RATE = 0.1;
  private static final double MUTATION_SIGMA = 0.5;

  private final RandomGenerator rng;
  private final Mutation mutation;
  private final Crossover crossover;

  public WeightsOnlyStrategy(RandomGenerator rng) {
    this.rng = rng;
    this.mutation = new Mutation(rng);
    this.crossover = new Crossover(rng);
  }

  @Override
  public List<Genome> evolve(List<Evaluated> population, int targetSize) {
    List<Evaluated> ranked = new ArrayList<>(population);
    ranked.sort(Comparator.comparingDouble(Evaluated::fitness).reversed());

    List<Genome> next = new ArrayList<>(targetSize);
    int elites = Math.max(1, (int) Math.round(targetSize * ELITE_FRACTION));
    for (int i = 0; i < elites && i < ranked.size(); i++) {
      next.add(ranked.get(i).genome());
    }
    while (next.size() < targetSize) {
      Genome child = crossover.cross(select(ranked), select(ranked));
      next.add(mutation.mutateWeights(child, MUTATION_RATE, MUTATION_SIGMA));
    }
    return next;
  }

  @Override
  public GenomeDistance distance() {
    return WeightDistance::euclidean;
  }

  private Genome select(List<Evaluated> ranked) {
    Evaluated best = ranked.get(rng.nextInt(ranked.size()));
    for (int i = 1; i < TOURNAMENT_SIZE; i++) {
      Evaluated candidate = ranked.get(rng.nextInt(ranked.size()));
      if (candidate.fitness() > best.fitness()) {
        best = candidate;
      }
    }
    return best.genome();
  }
}
