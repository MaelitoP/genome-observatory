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

class GeneticAlgorithmTest {

  private final List<Evaluated> population = population();

  @Test
  void producesRequestedPopulationSize() {
    List<Genome> next = new GeneticAlgorithm(new Random(1L)).evolve(population, 10);

    assertThat(next).hasSize(10);
  }

  @Test
  void carriesBestGenomeForwardAsElite() {
    Evaluated champion =
        population.stream().max(Comparator.comparingDouble(Evaluated::fitness)).orElseThrow();

    List<Genome> next = new GeneticAlgorithm(new Random(1L)).evolve(population, 10);

    assertThat(next).contains(champion.genome());
  }

  @Test
  void isReproducibleForAGivenSeed() {
    List<Genome> first = new GeneticAlgorithm(new Random(7L)).evolve(population, 10);
    List<Genome> second = new GeneticAlgorithm(new Random(7L)).evolve(population, 10);

    assertThat(first).isEqualTo(second);
  }

  private static List<Evaluated> population() {
    RandomGenerator rng = new Random(99L);
    List<Evaluated> evaluated = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Genome genome = Genome.initial(3, List.of(Activation.TANH), rng);
      evaluated.add(new Evaluated(genome, i));
    }
    return evaluated;
  }
}
