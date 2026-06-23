package dev.maelitop.evolution.core.neural;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NetworkPhenotype {

  private final int inputCount;
  private final int[] inputIndex;
  private final int biasIndex;
  private final int[] outputIndex;
  private final int[] evalOrder;
  private final int[][] incomingSource;
  private final double[][] incomingWeight;
  private final Activation[] activation;
  private final double[] values;

  private NetworkPhenotype(
      int inputCount,
      int[] inputIndex,
      int biasIndex,
      int[] outputIndex,
      int[] evalOrder,
      int[][] incomingSource,
      double[][] incomingWeight,
      Activation[] activation) {
    this.inputCount = inputCount;
    this.inputIndex = inputIndex;
    this.biasIndex = biasIndex;
    this.outputIndex = outputIndex;
    this.evalOrder = evalOrder;
    this.incomingSource = incomingSource;
    this.incomingWeight = incomingWeight;
    this.activation = activation;
    this.values = new double[activation.length];
  }

  public static NetworkPhenotype from(Genome genome) {
    List<NodeGene> nodes = genome.nodes();
    int n = nodes.size();
    Map<Integer, Integer> indexOf = new HashMap<>(n * 2);
    for (int i = 0; i < n; i++) {
      indexOf.put(nodes.get(i).id(), i);
    }

    List<Integer> inputs = new ArrayList<>();
    List<Integer> outputs = new ArrayList<>();
    int bias = -1;
    Activation[] activation = new Activation[n];
    for (int i = 0; i < n; i++) {
      NodeGene node = nodes.get(i);
      activation[i] = node.activation();
      switch (node.type()) {
        case INPUT -> inputs.add(i);
        case BIAS -> bias = i;
        case OUTPUT -> outputs.add(i);
        case HIDDEN -> {}
      }
    }

    List<List<Integer>> outAdjacency = new ArrayList<>(n);
    List<List<Integer>> inSources = new ArrayList<>(n);
    List<List<Double>> inWeights = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      outAdjacency.add(new ArrayList<>());
      inSources.add(new ArrayList<>());
      inWeights.add(new ArrayList<>());
    }
    int[] inDegree = new int[n];
    for (ConnectionGene gene : genome.connections()) {
      if (!gene.enabled()) {
        continue;
      }
      Integer source = indexOf.get(gene.in());
      Integer target = indexOf.get(gene.out());
      if (source == null || target == null) {
        throw new IllegalArgumentException(
            "connection references unknown node: in=" + gene.in() + " out=" + gene.out());
      }
      outAdjacency.get(source).add(target);
      inSources.get(target).add(source);
      inWeights.get(target).add(gene.weight());
      inDegree[target]++;
    }

    Deque<Integer> ready = new ArrayDeque<>();
    int[] remaining = inDegree.clone();
    for (int i = 0; i < n; i++) {
      if (remaining[i] == 0) {
        ready.add(i);
      }
    }
    List<Integer> topological = new ArrayList<>(n);
    while (!ready.isEmpty()) {
      int node = ready.poll();
      topological.add(node);
      for (int target : outAdjacency.get(node)) {
        if (--remaining[target] == 0) {
          ready.add(target);
        }
      }
    }
    if (topological.size() != n) {
      throw new IllegalArgumentException("genome network is not acyclic");
    }

    int[][] incomingSource = new int[n][];
    double[][] incomingWeight = new double[n][];
    for (int i = 0; i < n; i++) {
      List<Integer> sources = inSources.get(i);
      List<Double> weights = inWeights.get(i);
      incomingSource[i] = new int[sources.size()];
      incomingWeight[i] = new double[weights.size()];
      for (int j = 0; j < sources.size(); j++) {
        incomingSource[i][j] = sources.get(j);
        incomingWeight[i][j] = weights.get(j);
      }
    }

    List<Integer> eval = new ArrayList<>();
    for (int index : topological) {
      NodeType type = nodes.get(index).type();
      if (type != NodeType.INPUT && type != NodeType.BIAS) {
        eval.add(index);
      }
    }

    return new NetworkPhenotype(
        inputs.size(),
        toIntArray(inputs),
        bias,
        toIntArray(outputs),
        toIntArray(eval),
        incomingSource,
        incomingWeight,
        activation);
  }

  public double[] process(double[] inputs) {
    if (inputs.length != inputCount) {
      throw new IllegalArgumentException(
          "expected " + inputCount + " inputs but got " + inputs.length);
    }
    for (int k = 0; k < inputCount; k++) {
      values[inputIndex[k]] = inputs[k];
    }
    if (biasIndex >= 0) {
      values[biasIndex] = 1.0;
    }
    for (int node : evalOrder) {
      double sum = 0;
      int[] sources = incomingSource[node];
      double[] weights = incomingWeight[node];
      for (int j = 0; j < sources.length; j++) {
        sum += values[sources[j]] * weights[j];
      }
      values[node] = activation[node].apply(sum);
    }
    double[] outputs = new double[outputIndex.length];
    for (int k = 0; k < outputs.length; k++) {
      outputs[k] = values[outputIndex[k]];
    }
    return outputs;
  }

  private static int[] toIntArray(List<Integer> list) {
    int[] array = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      array[i] = list.get(i);
    }
    return array;
  }
}
