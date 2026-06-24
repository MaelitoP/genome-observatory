package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.ConnectionGene;
import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.neural.InnovationTracker;
import dev.maelitop.evolution.core.neural.NodeGene;
import dev.maelitop.evolution.core.neural.NodeType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

public final class Mutation {

  private static final Activation HIDDEN_ACTIVATION = Activation.TANH;
  private static final int CONNECTION_ATTEMPTS = 20;

  private final RandomGenerator rng;

  public Mutation(RandomGenerator rng) {
    this.rng = rng;
  }

  public Genome mutateWeights(Genome genome, double rate, double sigma) {
    List<ConnectionGene> mutated = new ArrayList<>(genome.connections().size());
    for (ConnectionGene gene : genome.connections()) {
      if (rng.nextDouble() < rate) {
        mutated.add(gene.withWeight(gene.weight() + rng.nextGaussian() * sigma));
      } else {
        mutated.add(gene);
      }
    }
    return new Genome(genome.nodes(), mutated);
  }

  public Genome addConnection(Genome genome, InnovationTracker tracker) {
    List<NodeGene> nodes = genome.nodes();
    Set<Long> existing = new HashSet<>();
    Map<Integer, List<Integer>> forward = new HashMap<>();
    for (ConnectionGene gene : genome.connections()) {
      existing.add(pair(gene.in(), gene.out()));
      if (gene.enabled()) {
        forward.computeIfAbsent(gene.in(), _ -> new ArrayList<>()).add(gene.out());
      }
    }

    for (int attempt = 0; attempt < CONNECTION_ATTEMPTS; attempt++) {
      NodeGene source = nodes.get(rng.nextInt(nodes.size()));
      NodeGene target = nodes.get(rng.nextInt(nodes.size()));
      if (target.type() == NodeType.INPUT || target.type() == NodeType.BIAS) {
        continue;
      }
      if (source.id() == target.id() || existing.contains(pair(source.id(), target.id()))) {
        continue;
      }
      if (reaches(forward, target.id(), source.id())) {
        continue;
      }
      List<ConnectionGene> connections = new ArrayList<>(genome.connections());
      connections.add(
          new ConnectionGene(
              source.id(),
              target.id(),
              rng.nextDouble(-1, 1),
              true,
              tracker.connection(source.id(), target.id())));
      return new Genome(nodes, connections);
    }
    return genome;
  }

  public Genome addNode(Genome genome, InnovationTracker tracker) {
    List<Integer> enabled = new ArrayList<>();
    List<ConnectionGene> connections = genome.connections();
    for (int i = 0; i < connections.size(); i++) {
      if (connections.get(i).enabled()) {
        enabled.add(i);
      }
    }
    if (enabled.isEmpty()) {
      return genome;
    }

    int splitIndex = enabled.get(rng.nextInt(enabled.size()));
    ConnectionGene split = connections.get(splitIndex);
    InnovationTracker.NodeSplit ids = tracker.split(split.innovation());

    List<NodeGene> nodes = new ArrayList<>(genome.nodes());
    nodes.add(new NodeGene(ids.newNodeId(), NodeType.HIDDEN, HIDDEN_ACTIVATION));

    List<ConnectionGene> rewired = new ArrayList<>(connections.size() + 2);
    rewired.addAll(connections);
    rewired.set(splitIndex, split.withEnabled(false));
    rewired.add(new ConnectionGene(split.in(), ids.newNodeId(), 1.0, true, ids.inToNew()));
    rewired.add(
        new ConnectionGene(ids.newNodeId(), split.out(), split.weight(), true, ids.newToOut()));
    return new Genome(nodes, rewired);
  }

  private static boolean reaches(Map<Integer, List<Integer>> forward, int start, int goal) {
    Deque<Integer> stack = new ArrayDeque<>();
    Set<Integer> seen = new HashSet<>();
    stack.push(start);
    while (!stack.isEmpty()) {
      int node = stack.pop();
      if (node == goal) {
        return true;
      }
      if (!seen.add(node)) {
        continue;
      }
      for (int next : forward.getOrDefault(node, List.of())) {
        stack.push(next);
      }
    }
    return false;
  }

  private static long pair(int in, int out) {
    return ((long) in << 32) | (out & 0xffffffffL);
  }
}
