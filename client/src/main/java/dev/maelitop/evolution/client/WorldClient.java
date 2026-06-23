package dev.maelitop.evolution.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.maelitop.evolution.protocol.GenerationSummary;
import dev.maelitop.evolution.protocol.HelloMessage;
import dev.maelitop.evolution.protocol.ProtocolJson;
import dev.maelitop.evolution.protocol.ServerMessage;
import dev.maelitop.evolution.protocol.WorldSnapshot;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Written by the WebSocket reader thread; read by the render thread via {@link AtomicReference}.
 */
final class WorldClient extends WebSocketClient {

  private static final Logger log = LoggerFactory.getLogger(WorldClient.class);

  private final AtomicReference<WorldSnapshot> snapshot = new AtomicReference<>();
  private final AtomicReference<HelloMessage> hello = new AtomicReference<>();

  WorldClient(URI serverUri) {
    super(serverUri);
  }

  @Override
  public void onOpen(ServerHandshake handshake) {
    log.info("connected to {}", getURI());
  }

  @Override
  public void onMessage(String message) {
    try {
      switch (ServerMessage.parse(ProtocolJson.mapper(), message)) {
        case HelloMessage h -> hello.set(h);
        case WorldSnapshot s -> snapshot.set(s);
        case GenerationSummary g -> log.info("generation {} complete", g.generation());
      }
    } catch (JsonProcessingException | IllegalArgumentException e) {
      log.warn("ignoring unrecognized frame", e);
    }
  }

  @Override
  public void onClose(int code, String reason, boolean remote) {
    log.info("connection closed: {}", reason);
  }

  @Override
  public void onError(Exception ex) {
    log.warn("websocket error", ex);
  }

  WorldSnapshot snapshot() {
    return snapshot.get();
  }

  HelloMessage hello() {
    return hello.get();
  }
}
