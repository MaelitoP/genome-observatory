package dev.maelitop.evolution.core.evolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.ConnectionGene;
import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.neural.NodeGene;
import dev.maelitop.evolution.core.neural.NodeType;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiversityTest {

  @Test
  void isZeroForIdenticalGenomes() {
    Genome g = genome(0.5);

    assertThat(Diversity.meanPairwiseDistance(List.of(g, g, g))).isZero();
  }

  @Test
  void averagesEuclideanWeightDistanceAcrossPairs() {
    assertThat(Diversity.meanPairwiseDistance(List.of(genome(0.0), genome(3.0)))).isEqualTo(3.0);
  }

  @Test
  void isZeroForSingleton() {
    assertThat(Diversity.meanPairwiseDistance(List.of(genome(1.0)))).isZero();
  }

  @Test
  void rejectsDivergentStructure() {
    Genome small = genome(0.0);
    Genome large =
        new Genome(
            small.nodes(),
            List.of(small.connections().get(0), small.connections().get(0).withWeight(1.0)));

    assertThatThrownBy(() -> Diversity.meanPairwiseDistance(List.of(small, large)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("divergent structure");
  }

  private static Genome genome(double weight) {
    return new Genome(
        List.of(
            new NodeGene(0, NodeType.BIAS, Activation.IDENTITY),
            new NodeGene(1, NodeType.OUTPUT, Activation.TANH)),
        List.of(new ConnectionGene(0, 1, weight, true, 0)));
  }
}
