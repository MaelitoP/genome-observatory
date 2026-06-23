package dev.maelitop.evolution.core.neural;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public record Genome(List<NodeGene> nodes, List<ConnectionGene> connections) {

  public Genome {
    nodes = List.copyOf(nodes);
    connections = List.copyOf(connections);
  }

  public static Genome initial(
      int inputCount, List<Activation> outputActivations, RandomGenerator rng) {
    List<NodeGene> nodes = new ArrayList<>(inputCount + 1 + outputActivations.size());
    for (int i = 0; i < inputCount; i++) {
      nodes.add(new NodeGene(i, NodeType.INPUT, Activation.IDENTITY));
    }
    int biasId = inputCount;
    nodes.add(new NodeGene(biasId, NodeType.BIAS, Activation.IDENTITY));

    int firstOutputId = inputCount + 1;
    List<ConnectionGene> connections = new ArrayList<>();
    int innovation = 0;
    for (int o = 0; o < outputActivations.size(); o++) {
      int outId = firstOutputId + o;
      nodes.add(new NodeGene(outId, NodeType.OUTPUT, outputActivations.get(o)));
      for (int source = 0; source <= biasId; source++) {
        connections.add(
            new ConnectionGene(source, outId, rng.nextDouble(-1, 1), true, innovation++));
      }
    }
    return new Genome(nodes, connections);
  }

  public NetworkPhenotype express() {
    return NetworkPhenotype.from(this);
  }
}
