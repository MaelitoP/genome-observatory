package dev.maelitop.evolution.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.persistence.RunStore;
import dev.maelitop.evolution.protocol.HelloMessage;
import dev.maelitop.evolution.protocol.WorldSnapshot;
import dev.maelitop.evolution.protocol.WorldStats;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ControlDispatchTest {

  private final TickControl control = new TickControl();
  private final RunStore store = new RunStore("jdbc:sqlite::memory:", new ObjectMapper());
  private final RunService runs = new RunService(store, WorldConfig.defaults());
  private final SimulationServer server = server();

  @AfterEach
  void close() {
    runs.close();
  }

  @Test
  void pauseFrameGatesTheTicker() {
    server.handleControl("{\"type\":\"PAUSE\"}");

    assertThat(control.allow()).isFalse();
  }

  @Test
  void stepFrameReleasesOneTickWhilePaused() {
    server.handleControl("{\"type\":\"PAUSE\"}");
    server.handleControl("{\"type\":\"STEP\"}");

    assertThat(control.allow()).isTrue();
    assertThat(control.allow()).isFalse();
  }

  @Test
  void resumeFrameUngatesTheTicker() {
    server.handleControl("{\"type\":\"PAUSE\"}");
    server.handleControl("{\"type\":\"RESUME\"}");

    assertThat(control.allow()).isTrue();
  }

  @Test
  void unrecognizedFrameIsIgnored() {
    server.handleControl("not json");

    assertThat(control.allow()).isTrue();
  }

  private SimulationServer server() {
    var config = new dev.maelitop.evolution.protocol.WorldConfig(100, 100);
    HelloMessage hello = new HelloMessage("test", 1L, config, 0L, 30);
    return new SimulationServer(0, hello, new FakeWorld(), runs, control);
  }

  private static final class FakeWorld implements SimulationWorld {
    @Override
    public void step() {}

    @Override
    public WorldSnapshot snapshot() {
      return new WorldSnapshot(0, 0, 0, new WorldStats(0, 0, 0), List.of());
    }
  }
}
