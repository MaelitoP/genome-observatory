package dev.maelitop.evolution.core.neural;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class GenomeTest {

  @Test
  void initialHasFixedInputBiasOutputStructure() {
    RandomGenerator rng = new Random(1L);
    Genome genome =
        Genome.initial(14, List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID), rng);

    assertThat(genome.nodes()).hasSize(18);
    assertThat(genome.nodes().stream().filter(n -> n.type() == NodeType.INPUT)).hasSize(14);
    assertThat(genome.nodes().stream().filter(n -> n.type() == NodeType.BIAS)).hasSize(1);
    assertThat(genome.nodes().stream().filter(n -> n.type() == NodeType.OUTPUT)).hasSize(3);
    assertThat(genome.connections()).hasSize(45);
    assertThat(genome.connections()).allMatch(ConnectionGene::enabled);
  }

  @Test
  void initialConnectsEveryInputAndBiasToEveryOutput() {
    Genome genome = Genome.initial(2, List.of(Activation.TANH), new Random(1L));

    assertThat(genome.connections()).hasSize(3);
    assertThat(genome.connections().stream().map(ConnectionGene::in))
        .containsExactlyInAnyOrder(0, 1, 2);
  }
}
