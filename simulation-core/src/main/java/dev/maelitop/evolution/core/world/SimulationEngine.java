package dev.maelitop.evolution.core.world;

import dev.maelitop.evolution.core.domain.Creature;
import dev.maelitop.evolution.core.domain.Entity;
import dev.maelitop.evolution.core.domain.Food;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.Vec2;
import dev.maelitop.evolution.core.domain.World;
import dev.maelitop.evolution.core.domain.WorldConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.random.RandomGenerator;

public final class SimulationEngine {

  public static final int INPUT_COUNT = 14;
  private static final double EAT_RADIUS = 12.0;

  private final WorldConfig config;
  private final RandomGenerator rng;
  private final Rectangle bounds;
  private final double dt;

  public SimulationEngine(WorldConfig config, RandomGenerator rng) {
    this.config = config;
    this.rng = rng;
    this.bounds = new Rectangle(0, 0, config.width() + 1.0, config.height() + 1.0);
    this.dt = 1.0 / config.tickRate();
  }

  public void step(World world) {
    Quadtree<Entity> index = new Quadtree<>(bounds, 8, 8);
    for (Food item : world.food()) {
      index.insert(item.position().x(), item.position().y(), item);
    }
    for (Creature creature : world.creatures()) {
      if (creature.alive()) {
        index.insert(creature.position().x(), creature.position().y(), creature);
      }
    }

    for (Creature creature : world.creatures()) {
      if (!creature.alive()) {
        continue;
      }
      double[] outputs = creature.brain().process(sense(creature, index));
      creature.act(outputs[0], outputs[1], outputs[2], config, dt);
    }

    feed(world);

    for (Creature creature : world.creatures()) {
      if (creature.alive()
          && (creature.energy() <= 0 || creature.age() >= config.maxAgeSeconds())) {
        creature.die();
      }
    }

    world.respawnFood(config, rng);
    world.advanceTick();
  }

  double[] sense(Creature self, Quadtree<Entity> index) {
    double range = config.visionRange();
    double halfFov = config.fieldOfView() / 2;
    Vec2 origin = self.position();
    Rectangle vision = new Rectangle(origin.x() - range, origin.y() - range, 2 * range, 2 * range);

    Entity leftNearest = null;
    Entity rightNearest = null;
    double leftDistance = range;
    double rightDistance = range;
    double leftBearing = 0;
    double rightBearing = 0;

    for (Entity other : index.query(vision)) {
      if (other == self) {
        continue;
      }
      double distance = origin.distanceTo(other.position());
      if (distance <= 0 || distance > range) {
        continue;
      }
      double bearing =
          normalizeAngle(
              Math.atan2(other.position().y() - origin.y(), other.position().x() - origin.x())
                  - self.heading());
      if (Math.abs(bearing) > halfFov) {
        continue;
      }
      if (bearing < 0) {
        if (leftNearest == null || distance < leftDistance) {
          leftNearest = other;
          leftDistance = distance;
          leftBearing = bearing;
        }
      } else if (rightNearest == null || distance < rightDistance) {
        rightNearest = other;
        rightDistance = distance;
        rightBearing = bearing;
      }
    }

    double[] inputs = new double[INPUT_COUNT];
    fillEye(inputs, 0, self.team(), leftNearest, leftDistance, leftBearing, range, halfFov);
    fillEye(inputs, 5, self.team(), rightNearest, rightDistance, rightBearing, range, halfFov);
    inputs[10] = self.energy() / config.maxEnergy();
    inputs[11] = self.age() / config.maxAgeSeconds();
    inputs[12] = self.speed() / config.maxSpeed();
    inputs[13] = self.spikeLength() / config.spikeMaxLength();
    return inputs;
  }

  private static void fillEye(
      double[] inputs,
      int offset,
      Team viewer,
      Entity target,
      double distance,
      double bearing,
      double range,
      double halfFov) {
    if (target == null) {
      inputs[offset] = 1.0;
      return;
    }
    inputs[offset] = distance / range;
    inputs[offset + 1] = bearing / halfFov;
    switch (target) {
      case Food ignored -> inputs[offset + 2] = 1.0;
      case Creature creature -> {
        if (creature.team() == viewer) {
          inputs[offset + 3] = 1.0;
        } else {
          inputs[offset + 4] = 1.0;
        }
      }
    }
  }

  private void feed(World world) {
    Quadtree<Food> foodIndex = new Quadtree<>(bounds, 8, 8);
    for (Food item : world.food()) {
      foodIndex.insert(item.position().x(), item.position().y(), item);
    }
    Set<Long> eaten = new HashSet<>();
    for (Creature creature : world.creatures()) {
      if (!creature.alive() || creature.team() != Team.HERBIVORE) {
        continue;
      }
      Vec2 at = creature.position();
      Rectangle reach =
          new Rectangle(at.x() - EAT_RADIUS, at.y() - EAT_RADIUS, 2 * EAT_RADIUS, 2 * EAT_RADIUS);
      for (Food item : foodIndex.query(reach)) {
        if (eaten.contains(item.id())) {
          continue;
        }
        if (at.distanceTo(item.position()) <= EAT_RADIUS) {
          creature.eat(config.foodEnergy(), config.maxEnergy());
          eaten.add(item.id());
        }
      }
    }
    world.removeFood(eaten);
  }

  private static double normalizeAngle(double angle) {
    double wrapped = angle % (2 * Math.PI);
    if (wrapped > Math.PI) {
      wrapped -= 2 * Math.PI;
    } else if (wrapped < -Math.PI) {
      wrapped += 2 * Math.PI;
    }
    return wrapped;
  }
}
