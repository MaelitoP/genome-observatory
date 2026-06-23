package dev.maelitop.evolution.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ClientMessageJsonTest {

  private final ObjectMapper mapper = ProtocolJson.mapper();

  @Test
  void roundTripsZeroFieldCommands() throws Exception {
    for (ClientMessage message : new ClientMessage[] {new Pause(), new Resume(), new Step()}) {
      String json = mapper.writeValueAsString(message);

      assertThat(mapper.readValue(json, ClientMessage.class)).isEqualTo(message);
    }
  }

  @Test
  void roundTripsSetSpeed() throws Exception {
    String json = mapper.writeValueAsString(new SetSpeed(2.5));

    assertThat(mapper.readValue(json, ClientMessage.class)).isEqualTo(new SetSpeed(2.5));
  }

  @Test
  void roundTripsSetParams() throws Exception {
    String json = mapper.writeValueAsString(new SetParams(1000, null));

    assertThat(mapper.readValue(json, ClientMessage.class)).isEqualTo(new SetParams(1000, null));
  }

  @Test
  void roundTripsWatchAgent() throws Exception {
    String json = mapper.writeValueAsString(new WatchAgent(42L));

    assertThat(mapper.readValue(json, ClientMessage.class)).isEqualTo(new WatchAgent(42L));
  }

  @Test
  void parsesByTypeDiscriminator() throws Exception {
    assertThat(mapper.readValue("{\"type\":\"PAUSE\"}", ClientMessage.class))
        .isInstanceOf(Pause.class);
  }

  @Test
  void rejectsNonPositiveSpeed() {
    assertThatThrownBy(() -> new SetSpeed(0)).isInstanceOf(IllegalArgumentException.class);
  }
}
