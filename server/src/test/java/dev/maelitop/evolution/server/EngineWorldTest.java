package dev.maelitop.evolution.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.LiveSimulation;
import dev.maelitop.evolution.core.evolution.WeightsOnlyStrategy;
import dev.maelitop.evolution.protocol.CreatureSnapshot;
import dev.maelitop.evolution.protocol.EntitySnapshot;
import dev.maelitop.evolution.protocol.FoodSnapshot;
import dev.maelitop.evolution.protocol.Team;
import dev.maelitop.evolution.protocol.WorldSnapshot;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class EngineWorldTest {

  private static EngineWorld engineWorld(int ticksPerGen) {
    WorldConfig config = WorldConfig.defaults();
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(1L);
    LiveSimulation live =
        new LiveSimulation(
            config,
            rng,
            new WeightsOnlyStrategy(rng),
            new WeightsOnlyStrategy(rng),
            6,
            4,
            ticksPerGen);
    return new EngineWorld(live, config);
  }

  @Test
  void snapshotContainsFoodAndAliveCreatures() {
    WorldSnapshot snap = engineWorld(100).snapshot();

    long creatures = snap.entities().stream().filter(e -> e instanceof CreatureSnapshot).count();
    long food = snap.entities().stream().filter(e -> e instanceof FoodSnapshot).count();
    assertThat(creatures).isEqualTo(10);
    assertThat(food).isEqualTo(WorldConfig.defaults().foodTarget());
    assertThat(snap.population()).isEqualTo(10);
  }

  @Test
  void creatureFieldsAreNormalized() {
    for (EntitySnapshot entity : engineWorld(100).snapshot().entities()) {
      if (entity instanceof CreatureSnapshot c) {
        assertThat(c.energy()).isBetween(0.0, 1.0);
        assertThat(c.spike()).isBetween(0.0, 1.0);
      }
    }
  }

  @Test
  void teamsMapToProtocolEnum() {
    WorldSnapshot snap = engineWorld(100).snapshot();

    long herbivores =
        snap.entities().stream()
            .filter(e -> e instanceof CreatureSnapshot c && c.team() == Team.HERBIVORE)
            .count();
    long carnivores =
        snap.entities().stream()
            .filter(e -> e instanceof CreatureSnapshot c && c.team() == Team.CARNIVORE)
            .count();
    assertThat(herbivores).isEqualTo(6);
    assertThat(carnivores).isEqualTo(4);
  }

  @Test
  void generationAdvancesInSnapshot() {
    int ticksPerGen = 20;
    EngineWorld world = engineWorld(ticksPerGen);
    for (int i = 0; i < ticksPerGen; i++) {
      world.step();
    }

    assertThat(world.snapshot().generation()).isEqualTo(1);
  }
}
