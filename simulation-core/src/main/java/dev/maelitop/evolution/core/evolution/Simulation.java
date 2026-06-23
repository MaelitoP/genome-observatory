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
  private final GeneticAlgorithm geneticAlgorithm;

  private List<Genome> population;
  private long nextCreatureId = 1;
  private int generation;

  public Simulation(WorldConfig config, RandomGenerator rng) {
    this.config = config;
    this.rng = rng;
    this.engine = new SimulationEngine(config, rng);
    this.geneticAlgorithm = new GeneticAlgorithm(rng);
    this.population = new ArrayList<>(config.population());
    for (int i = 0; i < config.population(); i++) {
      population.add(Genome.initial(SimulationEngine.INPUT_COUNT, OUTPUT_ACTIVATIONS, rng));
    }
  }

  public GenerationStats runGeneration() {
    World world = spawn();
    int ticks = (int) Math.round(config.generationSeconds() * config.tickRate());
    for (int t = 0; t < ticks && world.aliveCount() > 0; t++) {
      engine.step(world);
    }

    List<Evaluated> evaluated = new ArrayList<>(world.creatures().size());
    double best = Double.NEGATIVE_INFINITY;
    double sum = 0;
    for (Creature creature : world.creatures()) {
      double fitness = Fitness.of(creature, config.fitness());
      evaluated.add(new Evaluated(creature.genome(), fitness));
      best = Math.max(best, fitness);
      sum += fitness;
    }

    GenerationStats stats =
        new GenerationStats(
            generation,
            best,
            sum / evaluated.size(),
            median(evaluated),
            Diversity.meanPairwiseDistance(evaluated.stream().map(Evaluated::genome).toList()),
            evaluated.size());
    population = geneticAlgorithm.evolve(evaluated, config.population());
    generation++;
    return stats;
  }

  private static double median(List<Evaluated> evaluated) {
    double[] sorted = evaluated.stream().mapToDouble(Evaluated::fitness).sorted().toArray();
    int mid = sorted.length / 2;
    return sorted.length % 2 == 0 ? (sorted[mid - 1] + sorted[mid]) / 2.0 : sorted[mid];
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
