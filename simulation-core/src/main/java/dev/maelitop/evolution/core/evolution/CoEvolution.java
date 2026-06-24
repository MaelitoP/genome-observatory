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

public final class CoEvolution {

  private static final List<Activation> OUTPUT_ACTIVATIONS =
      List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID);

  private final WorldConfig config;
  private final RandomGenerator rng;
  private final SimulationEngine engine;
  private final EvolutionStrategy herbivoreStrategy;
  private final EvolutionStrategy carnivoreStrategy;
  private final int herbivoreCount;
  private final int carnivoreCount;

  private List<Genome> herbivores;
  private List<Genome> carnivores;
  private long nextCreatureId = 1;
  private int generation;

  public CoEvolution(
      WorldConfig config,
      RandomGenerator rng,
      EvolutionStrategy herbivoreStrategy,
      EvolutionStrategy carnivoreStrategy,
      int herbivoreCount,
      int carnivoreCount) {
    if (herbivoreCount <= 0 || carnivoreCount <= 0) {
      throw new IllegalArgumentException("both populations must be positive");
    }
    this.config = config;
    this.rng = rng;
    this.engine = new SimulationEngine(config, rng);
    this.herbivoreStrategy = herbivoreStrategy;
    this.carnivoreStrategy = carnivoreStrategy;
    this.herbivoreCount = herbivoreCount;
    this.carnivoreCount = carnivoreCount;
    this.herbivores = initialPopulation(herbivoreCount);
    this.carnivores = initialPopulation(carnivoreCount);
  }

  public CoEvolutionResult runGeneration() {
    World world = spawn();
    int ticks = (int) Math.round(config.generationSeconds() * config.tickRate());
    for (int t = 0; t < ticks && world.aliveCount() > 0; t++) {
      engine.step(world);
    }

    List<Evaluated> herbivoreScores = new ArrayList<>(herbivoreCount);
    List<Evaluated> carnivoreScores = new ArrayList<>(carnivoreCount);
    for (Creature creature : world.creatures()) {
      Evaluated scored = new Evaluated(creature.genome(), Fitness.of(creature, config.fitness()));
      if (creature.team() == Team.HERBIVORE) {
        herbivoreScores.add(scored);
      } else {
        carnivoreScores.add(scored);
      }
    }

    GenerationResult herbivoreResult =
        new GenerationResult(
            GenerationStats.summarize(generation, herbivoreScores, herbivoreStrategy.distance()),
            herbivoreScores);
    GenerationResult carnivoreResult =
        new GenerationResult(
            GenerationStats.summarize(generation, carnivoreScores, carnivoreStrategy.distance()),
            carnivoreScores);
    herbivores = herbivoreStrategy.evolve(herbivoreScores, herbivoreCount);
    carnivores = carnivoreStrategy.evolve(carnivoreScores, carnivoreCount);
    generation++;
    return new CoEvolutionResult(herbivoreResult, carnivoreResult);
  }

  private World spawn() {
    List<Creature> creatures = new ArrayList<>(herbivoreCount + carnivoreCount);
    for (Genome genome : herbivores) {
      creatures.add(creature(Team.HERBIVORE, genome));
    }
    for (Genome genome : carnivores) {
      creatures.add(creature(Team.CARNIVORE, genome));
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

  private Creature creature(Team team, Genome genome) {
    Vec2 position = new Vec2(rng.nextDouble(config.width()), rng.nextDouble(config.height()));
    return new Creature(
        nextCreatureId++, team, genome, position, rng.nextDouble(2 * Math.PI), config.maxEnergy());
  }

  private List<Genome> initialPopulation(int size) {
    List<Genome> population = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      population.add(Genome.initial(SimulationEngine.INPUT_COUNT, OUTPUT_ACTIVATIONS, rng));
    }
    return population;
  }
}
