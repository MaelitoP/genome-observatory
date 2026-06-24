package dev.maelitop.evolution.core.evolution;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.Genome;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class NeatStrategyTest {

  private static final int SIZE = 20;
  private final List<Evaluated> population = population();

  @Test
  void producesRequestedPopulationSize() {
    List<Genome> next = new NeatStrategy(new Random(1L)).evolve(population, SIZE);

    assertThat(next).hasSize(SIZE);
  }

  @Test
  void isReproducibleForAGivenSeed() {
    List<Genome> first = new NeatStrategy(new Random(7L)).evolve(population, SIZE);
    List<Genome> second = new NeatStrategy(new Random(7L)).evolve(population, SIZE);

    assertThat(first).isEqualTo(second);
  }

  @Test
  void carriesGlobalChampionForward() {
    Evaluated champion =
        population.stream().max(Comparator.comparingDouble(Evaluated::fitness)).orElseThrow();

    List<Genome> next = new NeatStrategy(new Random(1L)).evolve(population, SIZE);

    assertThat(next).contains(champion.genome());
  }

  @Test
  void growsTopologyWhenLargerGenomesAreFavoured() {
    NeatStrategy neat = new NeatStrategy(new Random(3L));
    List<Genome> generation = population().stream().map(Evaluated::genome).toList();

    for (int g = 0; g < 30; g++) {
      List<Evaluated> scored = new ArrayList<>(generation.size());
      for (Genome genome : generation) {
        scored.add(new Evaluated(genome, genome.nodes().size()));
      }
      generation = neat.evolve(scored, SIZE);
    }

    int largest = generation.stream().mapToInt(genome -> genome.nodes().size()).max().orElseThrow();
    assertThat(largest).isGreaterThan(18);
  }

  private static List<Evaluated> population() {
    RandomGenerator rng = new Random(99L);
    List<Evaluated> evaluated = new ArrayList<>(SIZE);
    for (int i = 0; i < SIZE; i++) {
      Genome genome =
          Genome.initial(14, List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID), rng);
      evaluated.add(new Evaluated(genome, i));
    }
    return evaluated;
  }
}
