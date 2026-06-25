package dev.maelitop.evolution.core.evolution;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.domain.Creature;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.Vec2;
import dev.maelitop.evolution.core.domain.WorldConfig;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class LiveSimulationTest {

  private static LiveSimulation liveSim(WorldConfig config, long seed, int ticksPerGen) {
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    return new LiveSimulation(
        config, rng, new WeightsOnlyStrategy(rng), new WeightsOnlyStrategy(rng), 6, 4, ticksPerGen);
  }

  @Test
  void stepAdvancesWorldTick() {
    LiveSimulation live = liveSim(WorldConfig.defaults(), 42L, 100);
    long before = live.world().tick();

    live.step();

    assertThat(live.world().tick()).isEqualTo(before + 1);
  }

  @Test
  void rollsOverToNextGenerationAfterBudget() {
    int ticksPerGen = 30;
    LiveSimulation live = liveSim(WorldConfig.defaults(), 42L, ticksPerGen);
    assertThat(live.generation()).isZero();

    for (int i = 0; i < ticksPerGen; i++) {
      live.step();
    }

    assertThat(live.generation()).isEqualTo(1);
    assertThat(live.world().tick()).isZero();
    assertThat(live.lastHerbivoreStats()).isPresent();
    assertThat(live.lastCarnivoreStats()).isPresent();
  }

  @Test
  void worldContainsBothTeams() {
    LiveSimulation live = liveSim(WorldConfig.defaults(), 7L, 100);

    long herbivores =
        live.world().creatures().stream().filter(c -> c.team() == Team.HERBIVORE).count();
    long carnivores =
        live.world().creatures().stream().filter(c -> c.team() == Team.CARNIVORE).count();

    assertThat(herbivores).isEqualTo(6);
    assertThat(carnivores).isEqualTo(4);
  }

  @Test
  void extinctionRollsOverWithoutThrowing() {
    LiveSimulation live = liveSim(shortLivedConfig(), 42L, 1000);

    live.step();

    assertThat(live.generation()).isEqualTo(1);
    assertThat(live.world().creatures()).hasSize(10);
    assertThat(live.lastHerbivoreStats()).isPresent();
    assertThat(live.lastCarnivoreStats()).isPresent();
  }

  @Test
  void deterministicForSameSeedAndStepCount() {
    LiveSimulation a = liveSim(WorldConfig.defaults(), 99L, 100);
    LiveSimulation b = liveSim(WorldConfig.defaults(), 99L, 100);

    for (int i = 0; i < 50; i++) {
      a.step();
      b.step();
    }

    assertThat(positions(a)).isEqualTo(positions(b));
  }

  private static List<Vec2> positions(LiveSimulation live) {
    return live.world().creatures().stream().map(Creature::position).toList();
  }

  private static WorldConfig shortLivedConfig() {
    WorldConfig d = WorldConfig.defaults();
    return new WorldConfig(
        d.width(),
        d.height(),
        d.tickRate(),
        d.foodTarget(),
        d.foodEnergy(),
        d.foodRespawnPerSecond(),
        d.population(),
        d.generationSeconds(),
        d.maxEnergy(),
        d.metabolicCost(),
        d.moveCost(),
        d.spikeCost(),
        0.001,
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
