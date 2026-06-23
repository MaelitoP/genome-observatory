package dev.maelitop.evolution.server;

import dev.maelitop.evolution.persistence.StoredAgent;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

final class RestApi {

  private RestApi() {}

  static void register(Javalin app, RunService service) {
    app.get("/runs", ctx -> ctx.json(service.runs()));
    app.post("/runs", ctx -> startRun(ctx, service));
    app.get("/runs/{id}/generations", ctx -> ctx.json(service.generations(id(ctx))));
    app.get(
        "/runs/{id}/stats",
        ctx -> ctx.json(service.stats(id(ctx)).orElseThrow(NotFoundResponse::new)));
    app.get(
        "/agents/{id}", ctx -> ctx.json(service.agent(id(ctx)).orElseThrow(NotFoundResponse::new)));
    app.get("/export/agents/{id}", ctx -> exportAgent(ctx, service));
  }

  private static void startRun(Context ctx, RunService service) {
    StartRunRequest request = ctx.bodyAsClass(StartRunRequest.class);
    if (request.generations() < 1) {
      throw new BadRequestResponse("generations must be at least 1");
    }
    long runId = service.start(request.seed(), request.generations());
    ctx.status(201).json(service.run(runId).orElseThrow());
  }

  private static void exportAgent(Context ctx, RunService service) {
    StoredAgent agent = service.agent(id(ctx)).orElseThrow(NotFoundResponse::new);
    ctx.header("Content-Disposition", "attachment; filename=agent-" + agent.id() + ".json");
    ctx.json(agent.genome());
  }

  private static long id(Context ctx) {
    return ctx.pathParamAsClass("id", Long.class).get();
  }
}
