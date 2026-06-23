package dev.maelitop.evolution.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.maelitop.evolution.protocol.HelloMessage;
import dev.maelitop.evolution.protocol.ProtocolJson;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimulationServer {

  private static final Logger log = LoggerFactory.getLogger(SimulationServer.class);

  private final int port;
  private final HelloMessage hello;
  private final BaselineWorld world;
  private final Set<WsContext> sessions = ConcurrentHashMap.newKeySet();
  private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor();
  private Javalin app;

  public SimulationServer(int port, HelloMessage hello, BaselineWorld world) {
    this.port = port;
    this.hello = hello;
    this.world = world;
  }

  public void start() {
    app =
        Javalin.create()
            .ws(
                "/world",
                ws -> {
                  ws.onConnect(
                      ctx -> {
                        sessions.add(ctx);
                        ctx.send(json(hello));
                      });
                  ws.onClose(ctx -> sessions.remove(ctx));
                });
    app.start(port);
    long periodMs = 1000L / hello.tickRate();
    var _ = ticker.scheduleAtFixedRate(this::tick, 0, periodMs, TimeUnit.MILLISECONDS);
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    log.info("Simulation server listening on ws://localhost:{}/world", port);
  }

  public void stop() {
    ticker.shutdown();
    try {
      if (!ticker.awaitTermination(2, TimeUnit.SECONDS)) {
        ticker.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ticker.shutdownNow();
    }
    if (app != null) {
      app.stop();
    }
  }

  private void tick() {
    world.step();
    String frame = json(world.snapshot());
    sessions.removeIf(ctx -> !trySend(ctx, frame));
  }

  private boolean trySend(WsContext ctx, String frame) {
    try {
      ctx.send(frame);
      return true;
    } catch (RuntimeException e) {
      log.debug("dropping unreachable session", e);
      return false;
    }
  }

  private static String json(Object value) {
    try {
      return ProtocolJson.mapper().writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
