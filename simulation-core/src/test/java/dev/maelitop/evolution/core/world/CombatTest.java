package dev.maelitop.evolution.core.world;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.domain.Creature;
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

class CombatTest {

  private static final Vec2 CENTER = new Vec2(1000, 1000);
  private static final Vec2 NEARBY = new Vec2(1000, 1010);

  private final WorldConfig config = WorldConfig.defaults();
  private final RandomGenerator rng = new Random(1L);

  @Test
  void carnivoreWithExtendedSpikeDamagesEnemyAndGainsEnergy() {
    Creature carnivore = creature(1, Team.CARNIVORE, brain(10.0), CENTER);
    Creature herbivore = creature(2, Team.HERBIVORE, brain(-10.0), NEARBY);
    World world = new World(List.of(carnivore, herbivore), List.of(), 1);

    new SimulationEngine(config, rng).step(world);

    assertThat(carnivore.energyGained()).isGreaterThan(0);
    assertThat(herbivore.energy()).isLessThan(config.maxEnergy() - config.metabolicCost());
  }

  @Test
  void retractedSpikeDealsNoDamage() {
    Creature carnivore = creature(1, Team.CARNIVORE, brain(-10.0), CENTER);
    Creature herbivore = creature(2, Team.HERBIVORE, brain(-10.0), NEARBY);
    World world = new World(List.of(carnivore, herbivore), List.of(), 1);

    new SimulationEngine(config, rng).step(world);

    assertThat(carnivore.energyGained()).isZero();
  }

  @Test
  void spikeDoesNotDamageSameTeam() {
    Creature attacker = creature(1, Team.CARNIVORE, brain(10.0), CENTER);
    Creature ally = creature(2, Team.CARNIVORE, brain(-10.0), NEARBY);
    World world = new World(List.of(attacker, ally), List.of(), 1);

    new SimulationEngine(config, rng).step(world);

    assertThat(attacker.energyGained()).isZero();
  }

  private Creature creature(long id, Team team, Genome brain, Vec2 position) {
    return new Creature(id, team, brain, position, 0, config.maxEnergy());
  }

  private static Genome brain(double biasToSpikeWeight) {
    List<NodeGene> nodes = new ArrayList<>();
    for (int i = 0; i < SimulationEngine.INPUT_COUNT; i++) {
      nodes.add(new NodeGene(i, NodeType.INPUT, Activation.IDENTITY));
    }
    int biasId = SimulationEngine.INPUT_COUNT;
    nodes.add(new NodeGene(biasId, NodeType.BIAS, Activation.IDENTITY));
    Activation[] outputs = {Activation.TANH, Activation.TANH, Activation.SIGMOID};
    List<ConnectionGene> connections = new ArrayList<>();
    int innovation = 0;
    for (int o = 0; o < outputs.length; o++) {
      int outId = biasId + 1 + o;
      nodes.add(new NodeGene(outId, NodeType.OUTPUT, outputs[o]));
      for (int source = 0; source <= biasId; source++) {
        double weight = o == 2 && source == biasId ? biasToSpikeWeight : 0.0;
        connections.add(new ConnectionGene(source, outId, weight, true, innovation++));
      }
    }
    return new Genome(nodes, connections);
  }
}
