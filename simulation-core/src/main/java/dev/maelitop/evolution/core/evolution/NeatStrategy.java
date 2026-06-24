package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.neural.CompatibilityDistance;
import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.neural.GenomeDistance;
import dev.maelitop.evolution.core.neural.InnovationTracker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

public final class NeatStrategy implements EvolutionStrategy {

  private static final double EXCESS_COEFFICIENT = 1.0;
  private static final double DISJOINT_COEFFICIENT = 1.0;
  private static final double WEIGHT_COEFFICIENT = 0.4;
  private static final double COMPATIBILITY_THRESHOLD = 3.0;
  private static final int TOURNAMENT_SIZE = 3;
  private static final double WEIGHT_MUTATION_RATE = 0.8;
  private static final double WEIGHT_MUTATION_SIGMA = 0.5;
  private static final double ADD_NODE_RATE = 0.03;
  private static final double ADD_CONNECTION_RATE = 0.05;

  private final RandomGenerator rng;
  private final Mutation mutation;
  private final Crossover crossover;
  private final CompatibilityDistance distance =
      new CompatibilityDistance(EXCESS_COEFFICIENT, DISJOINT_COEFFICIENT, WEIGHT_COEFFICIENT);

  private InnovationTracker tracker;
  private List<Genome> representatives = new ArrayList<>();

  public NeatStrategy(RandomGenerator rng) {
    this.rng = rng;
    this.mutation = new Mutation(rng);
    this.crossover = new Crossover(rng);
  }

  @Override
  public List<Genome> evolve(List<Evaluated> population, int targetSize) {
    if (tracker == null) {
      tracker =
          InnovationTracker.startingAfter(population.stream().map(Evaluated::genome).toList());
    }

    List<Species> species = speciate(population);
    int[] quota = allocate(species, targetSize);

    List<Genome> next = new ArrayList<>(targetSize);
    for (int i = 0; i < species.size(); i++) {
      Species group = species.get(i);
      if (quota[i] <= 0) {
        continue;
      }
      next.add(group.members().get(0).genome());
      for (int offspring = 1; offspring < quota[i]; offspring++) {
        next.add(breed(group));
      }
    }

    List<Genome> nextRepresentatives = new ArrayList<>(species.size());
    for (Species group : species) {
      nextRepresentatives.add(group.members().get(rng.nextInt(group.members().size())).genome());
    }
    representatives = nextRepresentatives;
    return next;
  }

  @Override
  public GenomeDistance distance() {
    return distance;
  }

  private List<Species> speciate(List<Evaluated> population) {
    List<Species> species = new ArrayList<>();
    for (Genome representative : representatives) {
      species.add(new Species(representative));
    }
    for (Evaluated candidate : population) {
      Species home = null;
      for (Species group : species) {
        if (distance.between(candidate.genome(), group.representative())
            < COMPATIBILITY_THRESHOLD) {
          home = group;
          break;
        }
      }
      if (home == null) {
        home = new Species(candidate.genome());
        species.add(home);
      }
      home.add(candidate);
    }
    species.removeIf(group -> group.members().isEmpty());
    for (Species group : species) {
      group.rankByFitness();
    }
    return species;
  }

  private int[] allocate(List<Species> species, int targetSize) {
    int count = species.size();
    double totalMean = 0.0;
    for (Species group : species) {
      totalMean += Math.max(0.0, group.meanFitness());
    }

    int[] quota = new int[count];
    double[] remainder = new double[count];
    int assigned = 0;
    for (int i = 0; i < count; i++) {
      double share =
          totalMean <= 0.0
              ? (double) targetSize / count
              : Math.max(0.0, species.get(i).meanFitness()) / totalMean * targetSize;
      quota[i] = (int) Math.floor(share);
      remainder[i] = share - quota[i];
      assigned += quota[i];
    }

    List<Integer> byRemainder = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      byRemainder.add(i);
    }
    byRemainder.sort(
        Comparator.comparingDouble((Integer i) -> remainder[i])
            .reversed()
            .thenComparingInt(i -> i));
    for (int i = 0; i < targetSize - assigned; i++) {
      quota[byRemainder.get(i)]++;
    }

    int champion = championSpecies(species);
    if (quota[champion] == 0) {
      int donor = 0;
      for (int i = 1; i < count; i++) {
        if (quota[i] > quota[donor]) {
          donor = i;
        }
      }
      quota[donor]--;
      quota[champion]++;
    }
    return quota;
  }

  private static int championSpecies(List<Species> species) {
    int champion = 0;
    double best = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < species.size(); i++) {
      double top = species.get(i).members().get(0).fitness();
      if (top > best) {
        best = top;
        champion = i;
      }
    }
    return champion;
  }

  private Genome breed(Species group) {
    List<Evaluated> members = group.members();
    Genome child;
    if (members.size() == 1) {
      child = members.get(0).genome();
    } else {
      Evaluated first = tournament(members);
      Evaluated second = tournament(members);
      boolean firstFitter = first.fitness() >= second.fitness();
      child =
          crossover.cross(
              firstFitter ? first.genome() : second.genome(),
              firstFitter ? second.genome() : first.genome());
    }
    child = mutation.mutateWeights(child, WEIGHT_MUTATION_RATE, WEIGHT_MUTATION_SIGMA);
    if (rng.nextDouble() < ADD_NODE_RATE) {
      child = mutation.addNode(child, tracker);
    }
    if (rng.nextDouble() < ADD_CONNECTION_RATE) {
      child = mutation.addConnection(child, tracker);
    }
    return child;
  }

  private Evaluated tournament(List<Evaluated> members) {
    Evaluated best = members.get(rng.nextInt(members.size()));
    for (int i = 1; i < TOURNAMENT_SIZE; i++) {
      Evaluated candidate = members.get(rng.nextInt(members.size()));
      if (candidate.fitness() > best.fitness()) {
        best = candidate;
      }
    }
    return best;
  }

  private static final class Species {

    private final Genome representative;
    private final List<Evaluated> members = new ArrayList<>();

    Species(Genome representative) {
      this.representative = representative;
    }

    Genome representative() {
      return representative;
    }

    List<Evaluated> members() {
      return members;
    }

    void add(Evaluated member) {
      members.add(member);
    }

    void rankByFitness() {
      members.sort(Comparator.comparingDouble(Evaluated::fitness).reversed());
    }

    double meanFitness() {
      double sum = 0.0;
      for (Evaluated member : members) {
        sum += member.fitness();
      }
      return members.isEmpty() ? 0.0 : sum / members.size();
    }
  }
}
