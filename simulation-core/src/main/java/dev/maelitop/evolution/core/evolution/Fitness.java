package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.domain.Creature;
import dev.maelitop.evolution.core.domain.FitnessWeights;

public final class Fitness {

  private Fitness() {}

  public static double of(Creature creature, FitnessWeights weights) {
    return weights.survival() * creature.survivalTicks()
        + weights.food() * creature.energyGained()
        + weights.explore() * creature.uniqueCellsVisited();
  }
}
