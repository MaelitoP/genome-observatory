package dev.maelitop.evolution.runner;

import dev.maelitop.evolution.core.evolution.EvolutionStrategy;
import dev.maelitop.evolution.core.evolution.NeatStrategy;
import dev.maelitop.evolution.core.evolution.WeightsOnlyStrategy;
import java.util.Locale;
import java.util.random.RandomGenerator;

public enum Strategy {
  WEIGHTS {
    @Override
    public EvolutionStrategy create(RandomGenerator rng) {
      return new WeightsOnlyStrategy(rng);
    }
  },
  NEAT {
    @Override
    public EvolutionStrategy create(RandomGenerator rng) {
      return new NeatStrategy(rng);
    }
  };

  public abstract EvolutionStrategy create(RandomGenerator rng);

  public static Strategy from(String name) {
    for (Strategy strategy : values()) {
      if (strategy.name().equalsIgnoreCase(name)) {
        return strategy;
      }
    }
    throw new IllegalArgumentException("unknown strategy: " + name);
  }

  public String flag() {
    return name().toLowerCase(Locale.ROOT);
  }
}
