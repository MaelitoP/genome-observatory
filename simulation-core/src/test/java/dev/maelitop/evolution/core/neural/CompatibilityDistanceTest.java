package dev.maelitop.evolution.core.neural;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilityDistanceTest {

  private final CompatibilityDistance distance = new CompatibilityDistance(1.0, 1.0, 0.4);

  @Test
  void identicalGenomesHaveZeroDistance() {
    Genome genome = genome(0.5, 0, 1, 2);

    assertThat(distance.between(genome, genome)).isZero();
  }

  @Test
  void weightDifferencesScaleByCoefficientAndMean() {
    assertThat(distance.between(genome(0.0, 0, 1), genome(2.0, 0, 1))).isEqualTo(0.8);
  }

  @Test
  void excessGenesAreCountedAndNormalized() {
    assertThat(distance.between(genome(0.0, 0, 1, 2), genome(0.0, 0, 1)))
        .isEqualTo(1.0 / 3.0, within(1e-9));
  }

  private static Genome genome(double weight, int... innovations) {
    List<NodeGene> nodes =
        List.of(
            new NodeGene(0, NodeType.BIAS, Activation.IDENTITY),
            new NodeGene(1, NodeType.OUTPUT, Activation.TANH));
    List<ConnectionGene> connections = new ArrayList<>();
    for (int innovation : innovations) {
      connections.add(new ConnectionGene(0, 1, weight, true, innovation));
    }
    return new Genome(nodes, connections);
  }
}
