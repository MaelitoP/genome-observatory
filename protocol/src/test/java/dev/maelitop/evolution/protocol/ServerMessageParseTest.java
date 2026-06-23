package dev.maelitop.evolution.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServerMessageParseTest {

  private final ObjectMapper mapper = ProtocolJson.mapper();

  @Test
  void routesHelloMessage() throws Exception {
    String json =
        mapper.writeValueAsString(new HelloMessage("r", 1L, new WorldConfig(10, 10), 0L, 30));

    assertThat(ServerMessage.parse(mapper, json)).isInstanceOf(HelloMessage.class);
  }

  @Test
  void routesWorldSnapshot() throws Exception {
    String json =
        mapper.writeValueAsString(new WorldSnapshot(1L, 0, 0, new WorldStats(0, 0, 0), List.of()));

    assertThat(ServerMessage.parse(mapper, json)).isInstanceOf(WorldSnapshot.class);
  }

  @Test
  void routesGenerationSummary() throws Exception {
    String json = mapper.writeValueAsString(new GenerationSummary(3, 1, 1, 0, 9L));

    assertThat(ServerMessage.parse(mapper, json)).isInstanceOf(GenerationSummary.class);
  }

  @Test
  void rejectsUnrecognizedFrame() {
    assertThatThrownBy(() -> ServerMessage.parse(mapper, "{\"foo\":1}"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonPositiveTickRate() {
    assertThatThrownBy(() -> new HelloMessage("r", 1L, new WorldConfig(10, 10), 0L, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
