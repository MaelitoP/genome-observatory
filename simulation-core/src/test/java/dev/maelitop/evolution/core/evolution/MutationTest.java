package dev.maelitop.evolution.core.evolution;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.Genome;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MutationTest {

  private final Genome genome = Genome.initial(3, List.of(Activation.TANH), new Random(0L));

  @Test
  void leavesWeightsUntouchedAtZeroRate() {
    Genome result = new Mutation(new Random(1L)).mutateWeights(genome, 0.0, 0.5);

    assertThat(result).isEqualTo(genome);
  }

  @Test
  void preservesStructure() {
    Genome result = new Mutation(new Random(1L)).mutateWeights(genome, 1.0, 0.5);

    assertThat(result.nodes()).isEqualTo(genome.nodes());
    assertThat(result.connections()).hasSameSizeAs(genome.connections());
  }

  @Test
  void perturbsWeightsDeterministicallyForASeed() {
    Genome first = new Mutation(new Random(2L)).mutateWeights(genome, 1.0, 0.5);
    Genome second = new Mutation(new Random(2L)).mutateWeights(genome, 1.0, 0.5);

    assertThat(first).isEqualTo(second).isNotEqualTo(genome);
  }
}
