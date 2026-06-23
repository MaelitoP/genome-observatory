package dev.maelitop.evolution.server;

import dev.maelitop.evolution.protocol.Color;
import dev.maelitop.evolution.protocol.CreatureSnapshot;
import dev.maelitop.evolution.protocol.EntitySnapshot;
import dev.maelitop.evolution.protocol.FoodSnapshot;
import dev.maelitop.evolution.protocol.Team;
import dev.maelitop.evolution.protocol.WorldConfig;
import dev.maelitop.evolution.protocol.WorldSnapshot;
import dev.maelitop.evolution.protocol.WorldStats;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Not thread-safe; confined to the single {@link SimulationServer} ticker thread. */
final class BaselineWorld {

  private final WorldConfig config;
  private final double dt;
  private final List<FoodSnapshot> food;
  private final Mob[] mobs;
  private long tick;

  BaselineWorld(
      WorldConfig config, RandomGenerator rng, int foodCount, int creatureCount, int tickRate) {
    this.config = config;
    this.dt = 1.0 / tickRate;
    this.food = new ArrayList<>(foodCount);
    for (int i = 0; i < foodCount; i++) {
      food.add(
          new FoodSnapshot(
              i + 1L, rng.nextDouble(config.width()), rng.nextDouble(config.height())));
    }
    this.mobs = new Mob[creatureCount];
    long firstCreatureId = foodCount + 1L;
    for (int i = 0; i < creatureCount; i++) {
      mobs[i] = Mob.random(firstCreatureId + i, config, rng);
    }
  }

  void step() {
    tick++;
    for (Mob mob : mobs) {
      mob.move(config, dt);
    }
  }

  WorldSnapshot snapshot() {
    List<EntitySnapshot> entities = new ArrayList<>(food.size() + mobs.length);
    entities.addAll(food);
    for (Mob mob : mobs) {
      entities.add(mob.snapshot(tick));
    }
    return new WorldSnapshot(tick, 0, mobs.length, new WorldStats(0, 0, 0), entities);
  }

  private static final class Mob {
    private final long id;
    private final Team team;
    private final Color color;
    private final double phase;
    private double x;
    private double y;
    private double vx;
    private double vy;

    private Mob(
        long id, Team team, Color color, double phase, double x, double y, double vx, double vy) {
      this.id = id;
      this.team = team;
      this.color = color;
      this.phase = phase;
      this.x = x;
      this.y = y;
      this.vx = vx;
      this.vy = vy;
    }

    static Mob random(long id, WorldConfig config, RandomGenerator rng) {
      double speed = 60 + rng.nextDouble(60);
      double dir = rng.nextDouble(2 * Math.PI);
      Color color = new Color(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
      return new Mob(
          id,
          Team.HERBIVORE,
          color,
          rng.nextDouble(2 * Math.PI),
          rng.nextDouble(config.width()),
          rng.nextDouble(config.height()),
          Math.cos(dir) * speed,
          Math.sin(dir) * speed);
    }

    void move(WorldConfig config, double dt) {
      x += vx * dt;
      y += vy * dt;
      if (x < 0 || x > config.width()) {
        vx = -vx;
        x = Math.clamp(x, 0, config.width());
      }
      if (y < 0 || y > config.height()) {
        vy = -vy;
        y = Math.clamp(y, 0, config.height());
      }
    }

    CreatureSnapshot snapshot(long tick) {
      double angle = Math.toDegrees(Math.atan2(vy, vx));
      double energy = 0.5 + 0.5 * Math.sin(tick * 0.05 + phase);
      return new CreatureSnapshot(id, x, y, angle, team, energy, 0, 0, color, 0, List.of());
    }
  }
}
