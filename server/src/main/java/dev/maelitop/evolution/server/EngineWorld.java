package dev.maelitop.evolution.server;

import dev.maelitop.evolution.core.domain.Creature;
import dev.maelitop.evolution.core.domain.Food;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.LiveSimulation;
import dev.maelitop.evolution.protocol.Color;
import dev.maelitop.evolution.protocol.CreatureSnapshot;
import dev.maelitop.evolution.protocol.EntitySnapshot;
import dev.maelitop.evolution.protocol.FoodSnapshot;
import dev.maelitop.evolution.protocol.WorldSnapshot;
import dev.maelitop.evolution.protocol.WorldStats;
import java.util.ArrayList;
import java.util.List;

/** Not thread-safe; confined to the single {@link SimulationServer} ticker thread. */
final class EngineWorld implements SimulationWorld {

  private final LiveSimulation live;
  private final WorldConfig config;

  EngineWorld(LiveSimulation live, WorldConfig config) {
    this.live = live;
    this.config = config;
  }

  @Override
  public void step() {
    live.step();
  }

  @Override
  public WorldSnapshot snapshot() {
    var world = live.world();
    List<EntitySnapshot> entities = new ArrayList<>();
    for (Food item : world.food()) {
      entities.add(new FoodSnapshot(item.id(), item.position().x(), item.position().y()));
    }
    int alive = 0;
    for (Creature creature : world.creatures()) {
      if (!creature.alive()) {
        continue;
      }
      alive++;
      entities.add(
          new CreatureSnapshot(
              creature.id(),
              creature.position().x(),
              creature.position().y(),
              Math.toDegrees(creature.heading()),
              protocolTeam(creature.team()),
              creature.energy() / config.maxEnergy(),
              creature.age(),
              creature.spikeLength() / config.spikeMaxLength(),
              colorFor(creature.team(), creature.id()),
              live.generation(),
              List.of()));
    }
    return new WorldSnapshot(world.tick(), live.generation(), alive, stats(), entities);
  }

  private WorldStats stats() {
    return live.lastHerbivoreStats()
        .map(s -> new WorldStats(s.bestFitness(), s.meanFitness(), s.diversity()))
        .orElse(new WorldStats(0, 0, 0));
  }

  private static dev.maelitop.evolution.protocol.Team protocolTeam(Team team) {
    return dev.maelitop.evolution.protocol.Team.valueOf(team.name());
  }

  private static Color colorFor(Team team, long id) {
    float jitter = ((id * 0x9E3779B1L) >>> 40) / (float) (1 << 24);
    return switch (team) {
      case HERBIVORE -> hsvColor(0.46f + 0.06f * jitter, 0.72f, 0.85f);
      case CARNIVORE -> hsvColor(0.95f + 0.06f * jitter, 0.55f, 0.98f);
    };
  }

  private static Color hsvColor(float h, float s, float v) {
    float hue6 = h * 6f;
    int sector = (int) Math.floor(hue6) % 6;
    float f = hue6 - (float) Math.floor(hue6);
    float p = v * (1 - s);
    float q = v * (1 - f * s);
    float t = v * (1 - (1 - f) * s);
    return switch (sector) {
      case 0 -> new Color(v, t, p);
      case 1 -> new Color(q, v, p);
      case 2 -> new Color(p, v, t);
      case 3 -> new Color(p, q, v);
      case 4 -> new Color(t, p, v);
      default -> new Color(v, p, q);
    };
  }
}
