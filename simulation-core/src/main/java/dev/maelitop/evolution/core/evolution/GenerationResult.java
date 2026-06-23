package dev.maelitop.evolution.core.evolution;

import java.util.List;

public record GenerationResult(GenerationStats stats, List<Evaluated> population) {

  public GenerationResult {
    population = List.copyOf(population);
  }
}
