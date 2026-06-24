package dev.maelitop.evolution.core.evolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.ConnectionGene;
import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.neural.InnovationTracker;
import dev.maelitop.evolution.core.neural.NodeGene;
import dev.maelitop.evolution.core.neural.NodeType;
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

  @Test
  void addNodeSplitsAnEnabledConnectionIntoAHiddenNode() {
    InnovationTracker tracker = InnovationTracker.startingAfter(genome);

    Genome result = new Mutation(new Random(1L)).addNode(genome, tracker);

    assertThat(result.nodes()).hasSize(genome.nodes().size() + 1);
    assertThat(result.nodes().stream().filter(n -> n.type() == NodeType.HIDDEN)).hasSize(1);
    assertThat(result.connections()).hasSize(genome.connections().size() + 2);
    assertThat(result.connections().stream().filter(c -> !c.enabled())).hasSize(1);
    assertThatCode(result::express).doesNotThrowAnyException();
  }

  @Test
  void addConnectionNeverCreatesACycle() {
    Genome saturated =
        new Genome(
            List.of(
                new NodeGene(0, NodeType.INPUT, Activation.IDENTITY),
                new NodeGene(1, NodeType.OUTPUT, Activation.TANH),
                new NodeGene(2, NodeType.HIDDEN, Activation.TANH)),
            List.of(
                new ConnectionGene(0, 2, 0.5, true, 0),
                new ConnectionGene(2, 1, 0.5, true, 1),
                new ConnectionGene(0, 1, 0.5, true, 2)));

    for (long seed = 0; seed < 50; seed++) {
      Genome result =
          new Mutation(new Random(seed))
              .addConnection(saturated, InnovationTracker.startingAfter(saturated));

      assertThat(result).isEqualTo(saturated);
      assertThatCode(result::express).doesNotThrowAnyException();
    }
  }

  @Test
  void addNodeReturnsGenomeUnchangedWhenNoEnabledConnections() {
    Genome disabled =
        new Genome(
            genome.nodes(), genome.connections().stream().map(c -> c.withEnabled(false)).toList());

    Genome result =
        new Mutation(new Random(1L)).addNode(disabled, InnovationTracker.startingAfter(disabled));

    assertThat(result).isEqualTo(disabled);
  }

  @Test
  void addConnectionAddsAnEnabledAcyclicLink() {
    Genome withHidden =
        new Genome(
            List.of(
                new NodeGene(0, NodeType.INPUT, Activation.IDENTITY),
                new NodeGene(1, NodeType.BIAS, Activation.IDENTITY),
                new NodeGene(2, NodeType.HIDDEN, Activation.TANH),
                new NodeGene(3, NodeType.OUTPUT, Activation.TANH)),
            List.of(
                new ConnectionGene(0, 3, 0.5, true, 0), new ConnectionGene(1, 3, 0.5, true, 1)));
    InnovationTracker tracker = InnovationTracker.startingAfter(withHidden);

    Genome result = new Mutation(new Random(4L)).addConnection(withHidden, tracker);

    assertThat(result.connections()).hasSize(withHidden.connections().size() + 1);
    assertThat(result.connections().get(result.connections().size() - 1).enabled()).isTrue();
    assertThatCode(result::express).doesNotThrowAnyException();
  }
}
