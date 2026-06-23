package dev.maelitop.evolution.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.persistence.RunStore;
import dev.maelitop.evolution.protocol.HelloMessage;
import dev.maelitop.evolution.protocol.WorldConfig;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public final class ServerMain {

  private static final int PORT = 7070;
  private static final int TICK_RATE = 30;
  private static final long SEED = 42L;
  private static final int FOOD_COUNT = 300;
  private static final int CREATURE_COUNT = 50;
  private static final String DB_PATH = "runs.db";

  private ServerMain() {}

  public static void main(String[] args) {
    WorldConfig config = new WorldConfig(2000, 2000);
    HelloMessage hello =
        new HelloMessage("baseline-" + SEED, SEED, config, System.currentTimeMillis(), TICK_RATE);
    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(SEED);
    BaselineWorld world = new BaselineWorld(config, rng, FOOD_COUNT, CREATURE_COUNT, TICK_RATE);
    RunStore store = new RunStore("jdbc:sqlite:" + DB_PATH, new ObjectMapper());
    RunService runs =
        new RunService(store, dev.maelitop.evolution.core.domain.WorldConfig.defaults());
    new SimulationServer(PORT, hello, world, runs).start();
  }
}
