package dev.maelitop.evolution.core.evolution;

import dev.maelitop.evolution.core.domain.Creature;
import dev.maelitop.evolution.core.domain.Food;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.Vec2;
import dev.maelitop.evolution.core.domain.World;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.world.SimulationEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public final class Simulation {

  private static final List<Activation> OUTPUT_ACTIVATIONS =
      List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID);

  private final WorldConfig config;
  private final RandomGenerator rng;
  private final SimulationEngine engine;
  private final EvolutionStrategy strategy;

  private List<Genome> population;
  private long nextCreatureId = 1;
  private int generation;

  public Simulation(WorldConfig config, RandomGenerator rng) {
    this(config, rng, new WeightsOnlyStrategy(rng));
  }

  public Simulation(WorldConfig config, RandomGenerator rng, EvolutionStrategy strategy) {
    this.config = config;
    this.rng = rng;
    this.engine = new SimulationEngine(config, rng);
    this.strategy = strategy;
    this.population = new ArrayList<>(config.population());
    for (int i = 0; i < config.population(); i++) {
      population.add(Genome.initial(SimulationEngine.INPUT_COUNT, OUTPUT_ACTIVATIONS, rng));
    }
  }

  public GenerationResult runGeneration() {
    World world = spawn();
    int ticks = (int) Math.round(config.generationSeconds() * config.tickRate());
    for (int t = 0; t < ticks && world.aliveCount() > 0; t++) {
      engine.step(world);
    }

    List<Evaluated> evaluated = new ArrayList<>(world.creatures().size());
    for (Creature creature : world.creatures()) {
      evaluated.add(new Evaluated(creature.genome(), Fitness.of(creature, config.fitness())));
    }

    GenerationStats stats = GenerationStats.summarize(generation, evaluated, strategy.distance());
    population = strategy.evolve(evaluated, config.population());
    generation++;
    return new GenerationResult(stats, evaluated);
  }

  private World spawn() {
    List<Creature> creatures = new ArrayList<>(population.size());
    for (Genome genome : population) {
      Vec2 position = new Vec2(rng.nextDouble(config.width()), rng.nextDouble(config.height()));
      creatures.add(
          new Creature(
              nextCreatureId++,
              Team.HERBIVORE,
              genome,
              position,
              rng.nextDouble(2 * Math.PI),
              config.maxEnergy()));
    }
    List<Food> food = new ArrayList<>(config.foodTarget());
    long foodId = 1;
    for (int i = 0; i < config.foodTarget(); i++) {
      food.add(
          new Food(
              foodId++, new Vec2(rng.nextDouble(config.width()), rng.nextDouble(config.height()))));
    }
    return new World(creatures, food, foodId);
  }
}
