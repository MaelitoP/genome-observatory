package dev.maelitop.evolution.core.neural;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InnovationTracker {

  public record NodeSplit(int newNodeId, int inToNew, int newToOut) {}

  private final Map<Long, Integer> connectionInnovations = new HashMap<>();
  private final Map<Integer, NodeSplit> splits = new HashMap<>();
  private int nextInnovation;
  private int nextNodeId;

  public InnovationTracker(int firstInnovation, int firstNodeId) {
    this.nextInnovation = firstInnovation;
    this.nextNodeId = firstNodeId;
  }

  public static InnovationTracker startingAfter(Genome genome) {
    return startingAfter(List.of(genome));
  }

  public static InnovationTracker startingAfter(List<Genome> genomes) {
    int maxNode = -1;
    int maxInnovation = -1;
    for (Genome genome : genomes) {
      for (NodeGene node : genome.nodes()) {
        maxNode = Math.max(maxNode, node.id());
      }
      for (ConnectionGene gene : genome.connections()) {
        maxInnovation = Math.max(maxInnovation, gene.innovation());
      }
    }
    return new InnovationTracker(maxInnovation + 1, maxNode + 1);
  }

  public int connection(int in, int out) {
    return connectionInnovations.computeIfAbsent(key(in, out), _ -> nextInnovation++);
  }

  public NodeSplit split(int splitConnectionInnovation) {
    return splits.computeIfAbsent(
        splitConnectionInnovation,
        _ -> new NodeSplit(nextNodeId++, nextInnovation++, nextInnovation++));
  }

  private static long key(int in, int out) {
    return ((long) in << 32) | (out & 0xffffffffL);
  }
}
