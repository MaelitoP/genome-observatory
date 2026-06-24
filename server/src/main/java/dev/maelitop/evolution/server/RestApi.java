package dev.maelitop.evolution.server;

import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.persistence.StoredAgent;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class RestApi {

  private RestApi() {}

  static void register(Javalin app, RunService service) {
    app.get("/runs", ctx -> ctx.json(service.runs()));
    app.post("/runs", ctx -> startRun(ctx, service));
    app.get("/runs/{id}/generations", ctx -> generations(ctx, service));
    app.get("/runs/{id}/stats", ctx -> stats(ctx, service));
    app.get("/runs/{id}/champion", ctx -> champion(ctx, service));
    app.get(
        "/agents/{id}", ctx -> ctx.json(service.agent(id(ctx)).orElseThrow(NotFoundResponse::new)));
    app.get("/export/agents/{id}", ctx -> exportAgent(ctx, service));
  }

  private static void startRun(Context ctx, RunService service) {
    StartRunRequest request = ctx.bodyAsClass(StartRunRequest.class);
    if (request.generations() < 1) {
      throw new BadRequestResponse("generations must be at least 1");
    }
    if (request.carnivores() < 0) {
      throw new BadRequestResponse("carnivores must not be negative");
    }
    long runId = service.start(request.seed(), request.generations(), request.carnivores());
    ctx.status(201).json(service.run(runId).orElseThrow());
  }

  private static void generations(Context ctx, RunService service) {
    long id = id(ctx);
    Optional<Team> team = teamParam(ctx);
    ctx.json(team.isPresent() ? service.generations(id, team.get()) : service.generations(id));
  }

  private static void stats(Context ctx, RunService service) {
    List<TeamRunStats> perTeam = service.stats(id(ctx)).orElseThrow(NotFoundResponse::new);
    ctx.json(
        teamParam(ctx)
            .map(team -> perTeam.stream().filter(entry -> entry.team() == team).toList())
            .orElse(perTeam));
  }

  private static void champion(Context ctx, RunService service) {
    Team team =
        teamParam(ctx)
            .orElseThrow(() -> new BadRequestResponse("team query parameter is required"));
    ctx.json(service.champion(id(ctx), team).orElseThrow(NotFoundResponse::new));
  }

  private static void exportAgent(Context ctx, RunService service) {
    StoredAgent agent = service.agent(id(ctx)).orElseThrow(NotFoundResponse::new);
    ctx.header("Content-Disposition", "attachment; filename=agent-" + agent.id() + ".json");
    ctx.json(agent.genome());
  }

  private static Optional<Team> teamParam(Context ctx) {
    String raw = ctx.queryParam("team");
    if (raw == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(Team.valueOf(raw.toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("unknown team: " + raw);
    }
  }

  private static long id(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}
