package dev.maelitop.evolution.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.maelitop.evolution.protocol.ClientMessage;
import dev.maelitop.evolution.protocol.GenerationSummary;
import dev.maelitop.evolution.protocol.HelloMessage;
import dev.maelitop.evolution.protocol.ProtocolJson;
import dev.maelitop.evolution.protocol.ServerMessage;
import dev.maelitop.evolution.protocol.WorldSnapshot;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the latest world state, written by the WebSocket reader thread and read by the render
 * thread via {@link AtomicReference}. A daemon poller opens a fresh connection whenever the current
 * one is closed, so the client recovers if it starts before the server or the link drops.
 */
final class WorldClient {

  private static final Logger log = LoggerFactory.getLogger(WorldClient.class);

  private final URI uri;
  private final AtomicReference<WorldSnapshot> snapshot = new AtomicReference<>();
  private final AtomicReference<HelloMessage> hello = new AtomicReference<>();
  private final ScheduledExecutorService reconnector =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread thread = new Thread(r, "ws-reconnect");
            thread.setDaemon(true);
            return thread;
          });

  private volatile Connection connection;
  private volatile boolean closed;

  WorldClient(URI uri) {
    this.uri = uri;
  }

  void start() {
    open();
    var _ = reconnector.scheduleWithFixedDelay(this::ensureConnected, 2, 2, TimeUnit.SECONDS);
  }

  private synchronized void open() {
    Connection c = new Connection();
    connection = c;
    c.connect();
  }

  private synchronized void ensureConnected() {
    if (closed) {
      return;
    }
    Connection c = connection;
    if (c == null || c.isClosed()) {
      if (c != null) {
        c.close();
      }
      open();
    }
  }

  void send(ClientMessage message) {
    Connection c = connection;
    if (c == null || !c.isOpen()) {
      return;
    }
    try {
      c.send(ProtocolJson.mapper().writeValueAsString(message));
    } catch (JsonProcessingException e) {
      log.warn("dropping unserializable control message {}", message, e);
    }
  }

  WorldSnapshot snapshot() {
    return snapshot.get();
  }

  HelloMessage hello() {
    return hello.get();
  }

  void close() {
    closed = true;
    reconnector.shutdownNow();
    Connection c = connection;
    if (c != null) {
      c.close();
    }
  }

  /** A single WebSocket connection attempt; replaced wholesale on reconnect. */
  private final class Connection extends WebSocketClient {

    Connection() {
      super(WorldClient.this.uri);
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
      log.debug("connection closed: {}", reason);
    }

    @Override
    public void onError(Exception ex) {
      log.debug("websocket error: {}", ex.toString());
    }
  }
}
