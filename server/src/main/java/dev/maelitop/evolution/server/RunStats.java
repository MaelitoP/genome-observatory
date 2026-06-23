package dev.maelitop.evolution.server;

import dev.maelitop.evolution.core.evolution.GenerationStats;
import java.util.List;

public record RunStats(
    long runId,
    int generations,
    double bestFitness,
    double finalMeanFitness,
    double finalDiversity) {

  static RunStats of(long runId, List<GenerationStats> history) {
    if (history.isEmpty()) {
      return new RunStats(runId, 0, 0, 0, 0);
    }
    double best = history.stream().mapToDouble(GenerationStats::bestFitness).max().orElse(0);
    GenerationStats last = history.getLast();
    return new RunStats(runId, history.size(), best, last.meanFitness(), last.diversity());
  }
}
