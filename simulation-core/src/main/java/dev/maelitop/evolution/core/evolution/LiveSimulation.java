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
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Continuous predator/prey evolution driven one tick at a time; confined to the caller of {@link
 * #step()}.
 */
public final class LiveSimulation {

  private static final List<Activation> OUTPUT_ACTIVATIONS =
      List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID);

  private final WorldConfig config;
  private final RandomGenerator rng;
  private final SimulationEngine engine;
  private final EvolutionStrategy herbivoreStrategy;
  private final EvolutionStrategy carnivoreStrategy;
  private final int herbivoreCount;
  private final int carnivoreCount;
  private final int ticksPerGeneration;

  private List<Genome> herbivores;
  private List<Genome> carnivores;
  private long nextCreatureId = 1;
  private int generation;
  private int ticksThisGeneration;
  private World world;
  private GenerationStats lastHerbivoreStats;
  private GenerationStats lastCarnivoreStats;

  public LiveSimulation(
      WorldConfig config,
      RandomGenerator rng,
      EvolutionStrategy herbivoreStrategy,
      EvolutionStrategy carnivoreStrategy,
      int herbivoreCount,
      int carnivoreCount,
      int ticksPerGeneration) {
    this.config = Objects.requireNonNull(config, "config");
    this.rng = Objects.requireNonNull(rng, "rng");
    this.herbivoreStrategy = Objects.requireNonNull(herbivoreStrategy, "herbivoreStrategy");
    this.carnivoreStrategy = Objects.requireNonNull(carnivoreStrategy, "carnivoreStrategy");
    if (herbivoreCount <= 0 || carnivoreCount <= 0) {
      throw new IllegalArgumentException("both populations must be positive");
    }
    if (ticksPerGeneration <= 0) {
      throw new IllegalArgumentException("ticksPerGeneration must be positive");
    }
    this.engine = new SimulationEngine(config, rng);
    this.herbivoreCount = herbivoreCount;
    this.carnivoreCount = carnivoreCount;
    this.ticksPerGeneration = ticksPerGeneration;
    this.herbivores = initialPopulation(herbivoreCount);
    this.carnivores = initialPopulation(carnivoreCount);
    this.world = spawn();
  }

  public void step() {
    engine.step(world);
    ticksThisGeneration++;
    if (ticksThisGeneration >= ticksPerGeneration || world.aliveCount() == 0) {
      rollover();
    }
  }

  public World world() {
    return world;
  }

  public int generation() {
    return generation;
  }

  public Optional<GenerationStats> lastHerbivoreStats() {
    return Optional.ofNullable(lastHerbivoreStats);
  }

  public Optional<GenerationStats> lastCarnivoreStats() {
    return Optional.ofNullable(lastCarnivoreStats);
  }

  private void rollover() {
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
    lastHerbivoreStats =
        GenerationStats.summarize(generation, herbivoreScores, herbivoreStrategy.distance());
    lastCarnivoreStats =
        GenerationStats.summarize(generation, carnivoreScores, carnivoreStrategy.distance());
    herbivores = herbivoreStrategy.evolve(herbivoreScores, herbivoreCount);
    carnivores = carnivoreStrategy.evolve(carnivoreScores, carnivoreCount);
    generation++;
    world = spawn();
  }

  private World spawn() {
    // Deliberately not shared with CoEvolution.spawn(): merging would couple this RNG draw
    // order to the batch classes' reproducibility contract.
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
    ticksThisGeneration = 0;
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
