package dev.maelitop.evolution.core.neural;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class NetworkPhenotypeTest {

  @Test
  void biasIsIncludedInWeightedSum() {
    Genome genome =
        new Genome(
            List.of(
                new NodeGene(0, NodeType.INPUT, Activation.IDENTITY),
                new NodeGene(1, NodeType.BIAS, Activation.IDENTITY),
                new NodeGene(2, NodeType.OUTPUT, Activation.IDENTITY)),
            List.of(
                new ConnectionGene(0, 2, 2.0, true, 0), new ConnectionGene(1, 2, 0.5, true, 1)));

    double[] output = genome.express().process(new double[] {3.0});

    assertThat(output[0]).isEqualTo(3.0 * 2.0 + 0.5);
  }

  @Test
  void evaluatesHiddenNodesInTopologicalOrder() {
    Genome genome =
        new Genome(
            List.of(
                new NodeGene(0, NodeType.INPUT, Activation.IDENTITY),
                new NodeGene(1, NodeType.OUTPUT, Activation.IDENTITY),
                new NodeGene(2, NodeType.HIDDEN, Activation.IDENTITY)),
            List.of(
                new ConnectionGene(0, 2, 3.0, true, 0), new ConnectionGene(2, 1, 2.0, true, 1)));

    double[] output = genome.express().process(new double[] {4.0});

    assertThat(output[0]).isEqualTo(4.0 * 3.0 * 2.0);
  }

  @Test
  void appliesPerNodeActivation() {
    Genome genome =
        new Genome(
            List.of(
                new NodeGene(0, NodeType.BIAS, Activation.IDENTITY),
                new NodeGene(1, NodeType.OUTPUT, Activation.SIGMOID)),
            List.of(new ConnectionGene(0, 1, 0.0, true, 0)));

    double[] output = genome.express().process(new double[] {});

    assertThat(output[0]).isEqualTo(0.5);
  }

  @Test
  void rejectsCyclicNetwork() {
    Genome genome =
        new Genome(
            List.of(
                new NodeGene(0, NodeType.INPUT, Activation.IDENTITY),
                new NodeGene(1, NodeType.OUTPUT, Activation.IDENTITY),
                new NodeGene(2, NodeType.HIDDEN, Activation.IDENTITY)),
            List.of(
                new ConnectionGene(0, 2, 1.0, true, 0),
                new ConnectionGene(2, 1, 1.0, true, 1),
                new ConnectionGene(1, 2, 1.0, true, 2)));

    assertThatThrownBy(genome::express)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("acyclic");
  }
}
