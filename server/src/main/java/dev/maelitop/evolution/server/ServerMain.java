package dev.maelitop.evolution.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.evolution.LiveSimulation;
import dev.maelitop.evolution.core.evolution.WeightsOnlyStrategy;
import dev.maelitop.evolution.persistence.RunStore;
import dev.maelitop.evolution.protocol.HelloMessage;
import dev.maelitop.evolution.protocol.WorldConfig;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public final class ServerMain {

  private static final int PORT = 7070;
  private static final int TICK_RATE = 30;
  private static final long SEED = 42L;
  private static final int HERBIVORES = 40;
  private static final int CARNIVORES = 10;
  private static final int TICKS_PER_GENERATION = 600;
  private static final String DB_PATH = "runs.db";

  private ServerMain() {}

  public static void main(String[] args) {
    WorldConfig clientView = new WorldConfig(2000, 2000);
    HelloMessage hello =
        new HelloMessage("live-" + SEED, SEED, clientView, System.currentTimeMillis(), TICK_RATE);

    RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(SEED);
    dev.maelitop.evolution.core.domain.WorldConfig engineConfig =
        dev.maelitop.evolution.core.domain.WorldConfig.defaults();
    LiveSimulation live =
        new LiveSimulation(
            engineConfig,
            rng,
            new WeightsOnlyStrategy(rng),
            new WeightsOnlyStrategy(rng),
            HERBIVORES,
            CARNIVORES,
            TICKS_PER_GENERATION);
    EngineWorld world = new EngineWorld(live, engineConfig);

    RunStore store = new RunStore("jdbc:sqlite:" + DB_PATH, new ObjectMapper());
    RunService runs =
        new RunService(store, dev.maelitop.evolution.core.domain.WorldConfig.defaults());
    new SimulationServer(PORT, hello, world, runs).start();
  }
}
