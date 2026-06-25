package dev.maelitop.evolution.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.maelitop.evolution.protocol.ClientMessage;
import dev.maelitop.evolution.protocol.HelloMessage;
import dev.maelitop.evolution.protocol.Pause;
import dev.maelitop.evolution.protocol.ProtocolJson;
import dev.maelitop.evolution.protocol.Resume;
import dev.maelitop.evolution.protocol.SetParams;
import dev.maelitop.evolution.protocol.SetSpeed;
import dev.maelitop.evolution.protocol.Step;
import dev.maelitop.evolution.protocol.WatchAgent;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimulationServer {

  private static final Logger log = LoggerFactory.getLogger(SimulationServer.class);

  private final int port;
  private final HelloMessage hello;
  private final SimulationWorld world;
  private final RunService runs;
  private final long basePeriodMs;
  private final Set<WsContext> sessions = ConcurrentHashMap.newKeySet();
  private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor();
  private final TickControl control;

  private volatile ScheduledFuture<?> tickHandle;
  private Javalin app;

  public SimulationServer(int port, HelloMessage hello, SimulationWorld world, RunService runs) {
    this(port, hello, world, runs, new TickControl());
  }

  SimulationServer(
      int port, HelloMessage hello, SimulationWorld world, RunService runs, TickControl control) {
    this.port = port;
    this.hello = hello;
    this.world = world;
    this.runs = runs;
    this.control = control;
    this.basePeriodMs = 1000L / hello.tickRate();
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
                  ws.onMessage(ctx -> handleControl(ctx.message()));
                  ws.onClose(ctx -> sessions.remove(ctx));
                });
    RestApi.register(app, runs);
    app.start(port);
    tickHandle = ticker.scheduleAtFixedRate(this::tick, 0, basePeriodMs, TimeUnit.MILLISECONDS);
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    log.info("Simulation server listening on http://localhost:{} (ws /world)", port);
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
    runs.close();
    if (app != null) {
      app.stop();
    }
  }

  void handleControl(String frame) {
    ClientMessage message;
    try {
      message = ProtocolJson.mapper().readValue(frame, ClientMessage.class);
    } catch (JsonProcessingException e) {
      log.warn("ignoring unrecognized control frame", e);
      return;
    }
    switch (message) {
      case Pause ignored -> control.pause();
      case Resume ignored -> control.resume();
      case Step ignored -> control.requestStep();
      case SetSpeed(double multiplier) -> reschedule(multiplier);
      case SetParams params -> log.info("live world has no editable config; ignoring {}", params);
      case WatchAgent watch -> log.info("watchAgent not yet supported: {}", watch.agentId());
    }
  }

  private synchronized void reschedule(double multiplier) {
    long period = Math.max(1, Math.round(basePeriodMs / multiplier));
    if (tickHandle != null) {
      tickHandle.cancel(false);
    }
    tickHandle = ticker.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.MILLISECONDS);
  }

  private void tick() {
    if (!control.allow()) {
      return;
    }
    try {
      world.step();
      String frame = json(world.snapshot());
      sessions.removeIf(ctx -> !trySend(ctx, frame));
    } catch (RuntimeException e) {
      log.error("simulation tick failed", e);
    }
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
