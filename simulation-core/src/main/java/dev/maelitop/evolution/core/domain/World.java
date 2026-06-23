package dev.maelitop.evolution.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

public final class World {

  private final List<Creature> creatures;
  private final List<Food> food;
  private long nextFoodId;
  private long tick;
  private double respawnAccumulator;

  public World(List<Creature> creatures, List<Food> food, long nextFoodId) {
    this.creatures = List.copyOf(creatures);
    this.food = new ArrayList<>(food);
    this.nextFoodId = nextFoodId;
  }

  public List<Creature> creatures() {
    return creatures;
  }

  public List<Food> food() {
    return Collections.unmodifiableList(food);
  }

  public long tick() {
    return tick;
  }

  public int aliveCount() {
    int count = 0;
    for (Creature creature : creatures) {
      if (creature.alive()) {
        count++;
      }
    }
    return count;
  }

  public void advanceTick() {
    tick++;
  }

  public void removeFood(Set<Long> ids) {
    if (!ids.isEmpty()) {
      food.removeIf(item -> ids.contains(item.id()));
    }
  }

  public void respawnFood(WorldConfig config, RandomGenerator rng) {
    respawnAccumulator += config.foodRespawnPerSecond() / config.tickRate();
    while (respawnAccumulator >= 1 && food.size() < config.foodTarget()) {
      food.add(
          new Food(
              nextFoodId++,
              new Vec2(rng.nextDouble(config.width()), rng.nextDouble(config.height()))));
      respawnAccumulator -= 1;
    }
  }
}
