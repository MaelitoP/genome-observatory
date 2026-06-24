package dev.maelitop.evolution.core.world;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.domain.Creature;
import dev.maelitop.evolution.core.domain.Food;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.Vec2;
import dev.maelitop.evolution.core.domain.World;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.ConnectionGene;
import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.neural.NodeGene;
import dev.maelitop.evolution.core.neural.NodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class SimulationEngineTest {

  private static final Vec2 CENTER = new Vec2(1000, 1000);

  private final WorldConfig config = WorldConfig.defaults();
  private final RandomGenerator rng = new Random(1L);

  @Test
  void metabolismDrainsEnergyEachStep() {
    Creature creature = stationaryCreature(config.maxEnergy());
    World world = new World(List.of(creature), List.of(), 1);

    new SimulationEngine(config, rng).step(world);

    assertThat(creature.energy()).isLessThan(config.maxEnergy());
    assertThat(creature.alive()).isTrue();
  }

  @Test
  void herbivoreEatsOverlappingFood() {
    Creature creature = stationaryCreature(50.0);
    World world = new World(List.of(creature), List.of(new Food(1, CENTER)), 2);

    new SimulationEngine(config, rng).step(world);

    assertThat(creature.energyGained()).isEqualTo(config.foodEnergy());
    assertThat(world.food()).isEmpty();
  }

  @Test
  void creatureDiesWhenEnergyDepleted() {
    Creature creature = stationaryCreature(0.05);
    World world = new World(List.of(creature), List.of(), 1);

    new SimulationEngine(config, rng).step(world);

    assertThat(creature.alive()).isFalse();
  }

  @Test
  void creatureDiesOfOldAge() {
    Creature creature = stationaryCreature(config.maxEnergy());
    World world = new World(List.of(creature), List.of(), 1);

    new SimulationEngine(agingConfig(), rng).step(world);

    assertThat(creature.alive()).isFalse();
    assertThat(creature.energy()).isGreaterThan(0);
  }

  private Creature stationaryCreature(double energy) {
    return new Creature(1, Team.HERBIVORE, stationaryBrain(), CENTER, 0, energy);
  }

  private static Genome stationaryBrain() {
    List<NodeGene> nodes = new ArrayList<>();
    for (int i = 0; i < SimulationEngine.INPUT_COUNT; i++) {
      nodes.add(new NodeGene(i, NodeType.INPUT, Activation.IDENTITY));
    }
    int biasId = SimulationEngine.INPUT_COUNT;
    nodes.add(new NodeGene(biasId, NodeType.BIAS, Activation.IDENTITY));
    List<Activation> outputs = List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID);
    List<ConnectionGene> connections = new ArrayList<>();
    int innovation = 0;
    for (int o = 0; o < outputs.size(); o++) {
      int outId = biasId + 1 + o;
      nodes.add(new NodeGene(outId, NodeType.OUTPUT, outputs.get(o)));
      for (int source = 0; source <= biasId; source++) {
        connections.add(new ConnectionGene(source, outId, 0.0, true, innovation++));
      }
    }
    return new Genome(nodes, connections);
  }

  private static WorldConfig agingConfig() {
    WorldConfig d = WorldConfig.defaults();
    return new WorldConfig(
        d.width(),
        d.height(),
        d.tickRate(),
        d.foodTarget(),
        d.foodEnergy(),
        d.foodRespawnPerSecond(),
        d.population(),
        1.0,
        d.maxEnergy(),
        0.0,
        d.moveCost(),
        d.spikeCost(),
        0.01,
        d.maxSpeed(),
        d.maxTurnRate(),
        d.visionRange(),
        d.fieldOfView(),
        d.spikeMaxLength(),
        d.spikeDamagePerSecond(),
        d.carnivoreEnergyRecovery(),
        d.fitness());
  }
}
