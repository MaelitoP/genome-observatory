package dev.maelitop.evolution.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.Theme;
import dev.maelitop.evolution.client.view.FitnessChart;
import dev.maelitop.evolution.client.view.HintBar;
import dev.maelitop.evolution.client.view.HudBar;
import dev.maelitop.evolution.client.view.NetworkRenderer;
import dev.maelitop.evolution.protocol.Team;
import java.util.List;

final class AnalyticsScreen extends ObservatoryScreen {

  private static final float ROW_H = 64f;

  private final RunBrowser runs;
  private final FitnessChart chart = new FitnessChart();
  private final NetworkRenderer net = new NetworkRenderer();
  private final float[] best = new float[4096];
  private final float[] mean = new float[4096];
  private final float[] div = new float[4096];
  private float sincePoll;

  AnalyticsScreen(EvolutionClient app, RunBrowser runs) {
    super(app);
    this.runs = runs;
  }

  @Override
  public void show() {
    super.show();
    runs.refresh();
  }

  @Override
  protected boolean modalOpen() {
    return runs.form().open();
  }

  @Override
  protected void input(float delta) {
    if (runs.form().open()) {
      formInput();
      return;
    }
    if (Gdx.input.isKeyJustPressed(Keys.UP)) {
      runs.selectPrev();
    }
    if (Gdx.input.isKeyJustPressed(Keys.DOWN)) {
      runs.selectNext();
    }
    if (Gdx.input.isKeyJustPressed(Keys.T)) {
      runs.toggleTeam();
    }
    if (Gdx.input.isKeyJustPressed(Keys.C)) {
      runs.toggleCompareSelected();
    }
    if (Gdx.input.isKeyJustPressed(Keys.N)) {
      runs.form().toggle();
    }
    if (Gdx.input.isKeyJustPressed(Keys.E)) {
      runs.exportChampion();
    }
    if (Gdx.input.isKeyJustPressed(Keys.R)) {
      runs.refresh();
    }
    if (Gdx.input.justTouched()) {
      handleClick(Gdx.input.getX(), height - Gdx.input.getY());
    }
  }

  private void handleClick(float mx, float my) {
    if (hits(newButtonRect(), mx, my)) {
      runs.form().toggle();
      return;
    }
    if (hits(exportButtonRect(), mx, my)) {
      runs.exportChampion();
      return;
    }
    if (mx >= 0 && mx <= Theme.RUNLIST_W) {
      float listTop = bodyY + bodyH - Theme.PANEL_PAD - 30f;
      int idx = (int) ((listTop - my) / ROW_H);
      if (idx >= 0 && idx < runs.runs().size()) {
        runs.select(idx);
      }
    }
  }

  private static boolean hits(float[] r, float mx, float my) {
    return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
  }

  private float[] newButtonRect() {
    float pad = Theme.PANEL_PAD;
    float bw = rc.fonts.width(rc.fonts.sansButton, "+ new") + 20f;
    float top = bodyY + bodyH - pad;
    return new float[] {Theme.RUNLIST_W - pad - bw, top - 15f, bw, 20f};
  }

  private float[] exportButtonRect() {
    float pad = Theme.PANEL_PAD;
    float champX = bodyW - Theme.CHAMPION_W;
    return new float[] {champX + pad, bodyY + pad, Theme.CHAMPION_W - pad * 2, 30f};
  }

  private void formInput() {
    StartRunForm form = runs.form();
    if (Gdx.input.isKeyJustPressed(Keys.UP)) {
      form.prevField();
    }
    if (Gdx.input.isKeyJustPressed(Keys.DOWN)) {
      form.nextField();
    }
    for (int key = Keys.NUM_0; key <= Keys.NUM_9; key++) {
      if (Gdx.input.isKeyJustPressed(key)) {
        form.typeDigit(key - Keys.NUM_0);
      }
    }
    if (Gdx.input.isKeyJustPressed(Keys.BACKSPACE)) {
      form.backspace();
    }
    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      runs.startRun();
    }
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      form.close();
    }
  }

  @Override
  protected void drawBody(float delta) {
    runs.sync();
    sincePoll += delta;
    if (sincePoll >= 1.5f) {
      sincePoll = 0f;
      runs.poll();
    }
    float listW = Theme.RUNLIST_W;
    float champW = Theme.CHAMPION_W;
    float chartX = listW;
    float chartW = bodyW - listW - champW;

    drawRunList(0, bodyY, listW, bodyH);
    if (runs.runs().isEmpty()) {
      drawEmpty(chartX, bodyY, chartW, bodyH);
    } else {
      drawChart(chartX, bodyY, chartW, bodyH);
    }
    drawChampion(bodyW - champW, bodyY, champW, bodyH);

    if (runs.form().open()) {
      NewRunModal.draw(rc, width, height, runs.form());
    }
  }

  @Override
  protected void drawChrome() {
    HudBar.draw(rc, width, height, "analytics", List.of(), null);
    HintBar.draw(
        rc,
        width,
        List.of(
            new HintBar.Hint("N", "new run"),
            new HintBar.Hint("T", "team"),
            new HintBar.Hint("E", "export"),
            new HintBar.Hint("R", "refresh")),
        new HintBar.Hint("TAB", "network"));
  }

  private void drawRunList(float x, float y, float w, float h) {
    Draw d = rc.draw;
    d.rect(x, y, w, h, Theme.PANEL);
    d.line(x + w, y, x + w, y + h, Theme.HAIRLINE);
    float pad = Theme.PANEL_PAD;
    float top = y + h - pad;

    d.tracked(rc.fonts.mono10, "RUNS", x + pad, top - 2f, 1.4f, Theme.TEXT_MUTED);
    float[] b = newButtonRect();
    d.button(b[0], b[1], b[2], b[3], "+ new", rc.fonts.sansButton, Theme.ACCENT, null, Theme.FIELD);

    List<RunSummary> list = runs.runs();
    float rowTop = top - 30f;
    for (int i = 0; i < list.size(); i++) {
      drawRunRow(d, list.get(i), x, rowTop - i * ROW_H, w, i == runs.selectedIndex());
    }

    float fy = y + 12f;
    float fx = x + pad;
    fx += d.keyCap(fx, fy, "↑↓") + 6f;
    d.textMid(rc.fonts.mono11, "select", fx, fy + 9f, Theme.TEXT_MUTED);
    fx += rc.fonts.width(rc.fonts.mono11, "select") + 16f;
    fx += d.keyCap(fx, fy, "C") + 6f;
    d.textMid(rc.fonts.mono11, "compare", fx, fy + 9f, Theme.TEXT_MUTED);
  }

  private void drawRunRow(
      Draw d, RunSummary run, float x, float rowTop, float w, boolean selected) {
    float pad = Theme.PANEL_PAD;
    float rowY = rowTop - ROW_H;
    boolean compared = runs.compared().contains(run.id());
    if (selected) {
      d.roundedRect(x + 6, rowY + 4, w - 12, ROW_H - 8, 5f, Theme.SELECT_TINT);
      d.strokeRoundedRect(x + 6, rowY + 4, w - 12, ROW_H - 8, 5f, Theme.ACCENT, 0.8f);
    }
    if (compared) {
      d.roundedRect(x + 6, rowY + 4, 2.5f, ROW_H - 8, 1f, Theme.DIVERSITY);
    }
    float line1 = rowTop - 26f;
    float line2 = rowTop - 46f;
    d.textMid(rc.fonts.mono14, runLabel(run), x + pad, line1, Theme.TEXT_PRIMARY);

    float rightX = x + w - pad;
    if (run.carnivores() > 0) {
      String badge = "CO-EVO";
      float bw = rc.fonts.width(rc.fonts.mono10, badge) + 14f;
      d.strokeRoundedRect(rightX - bw, line1 - 8f, bw, 17f, 4f, Theme.CARNIVORE, 0.8f);
      d.textMid(rc.fonts.mono10, badge, rightX - bw + 7f, line1, Theme.CARNIVORE);
    } else if (compared) {
      d.textMidRight(rc.fonts.mono11, "compare", rightX, line1, Theme.DIVERSITY);
    } else {
      d.textMidRight(rc.fonts.mono11, "herb-only", rightX, line1, Theme.TEXT_FAINT_3);
    }

    switch (run.status()) {
      case RUNNING -> {
        float dotX = x + pad + 4f;
        float pulse = 2.6f + 1.2f * (float) Math.sin(rc.time() * 4f);
        d.glowDisc(dotX, line2, pulse, Theme.STATUS_RUNNING, 1f);
        d.circle(dotX, line2, 2.4f, Theme.STATUS_RUNNING);
        d.textMid(
            rc.fonts.mono11,
            "gen " + run.currentGeneration() + "/" + run.generations(),
            dotX + 10f,
            line2,
            Theme.STATUS_RUNNING);
        float frac =
            run.generations() <= 0
                ? 0f
                : Math.max(0f, Math.min(1f, run.currentGeneration() / (float) run.generations()));
        float barX = x + pad;
        float barW = w - pad * 2 - 12f;
        float barY = rowY + 7f;
        d.roundedRect(barX, barY, barW, 3f, 1.5f, Theme.PANEL_ALT);
        if (frac > 0) {
          d.roundedRect(barX, barY, Math.max(frac * barW, 3f), 3f, 1.5f, Theme.STATUS_RUNNING);
        }
      }
      case QUEUED -> {
        int ahead = runs.aheadOf(run);
        d.textMid(
            rc.fonts.mono11,
            ahead > 0 ? "queued · " + ahead + " ahead" : "queued",
            x + pad,
            line2,
            Theme.PAUSED);
      }
      case INTERRUPTED ->
          d.textMid(
              rc.fonts.mono11,
              "interrupted · gen " + run.currentGeneration() + "/" + run.generations(),
              x + pad,
              line2,
              Theme.TEXT_FAINT_3);
      case COMPLETED ->
          d.textMid(
              rc.fonts.mono11,
              run.generations() + " gen · seed " + seed(run.seed()),
              x + pad,
              line2,
              Theme.TEXT_FAINT);
    }
  }

  private String subStatus(RunSummary run) {
    if (run == null) {
      return "";
    }
    int cur = run.currentGeneration();
    int target = run.generations();
    return switch (run.status()) {
      case RUNNING -> "computing · gen " + cur + "/" + target;
      case QUEUED -> {
        int ahead = runs.aheadOf(run);
        yield ahead > 0 ? "queued · " + ahead + " ahead" : "queued";
      }
      case COMPLETED -> "run complete · " + target + " generations";
      case INTERRUPTED -> "interrupted · gen " + cur + "/" + target;
    };
  }

  private void drawChart(float x, float y, float w, float h) {
    Draw d = rc.draw;
    float pad = Theme.PANEL_PAD;
    long runId = runs.selectedRunId();
    RunSummary run = runs.selectedRun().orElse(null);
    List<GenerationRecordView.Stats> pts = runs.teamPoints(runId);

    d.text(
        rc.fonts.sansTitle, "Fitness — " + runLabel(run), x + pad, y + h - pad, Theme.TEXT_PRIMARY);
    d.text(rc.fonts.mono11, subStatus(run), x + pad, y + h - pad - 20f, Theme.TEXT_MUTED);

    drawTeamToggle(x + w - pad, y + h - pad - 6f);

    chart.legend(
        rc,
        x + pad,
        y + h - pad - 44f,
        new String[] {"best", "mean", "diversity"},
        new Color[] {Theme.BEST, Theme.MEAN, Theme.DIVERSITY});

    if (pts.size() < 2) {
      d.textCentered(rc.fonts.mono12, "no data yet", x + w / 2f, y + h / 2f, Theme.TEXT_FAINT);
      return;
    }
    int n = pts.size();
    float fitMax = 1e-6f;
    float divMax = 1e-6f;
    for (int i = 0; i < n; i++) {
      best[i] = (float) pts.get(i).bestFitness();
      mean[i] = (float) pts.get(i).meanFitness();
      div[i] = (float) pts.get(i).diversity();
      fitMax = Math.max(fitMax, Math.max(best[i], mean[i]));
      divMax = Math.max(divMax, div[i]);
    }
    fitMax *= 1.08f;
    divMax *= 1.08f;
    for (int i = 0; i < n; i++) {
      best[i] /= fitMax;
      mean[i] /= fitMax;
      div[i] /= divMax;
    }
    FitnessChart.Plot p = chart.frame(rc, x + pad, y + pad, w - pad * 2, h - pad * 2 - 64f, n, 1f);
    chart.series(rc, p, div, n, Theme.DIVERSITY, false, 1.6f);
    chart.series(rc, p, mean, n, Theme.MEAN, false, 1.6f);
    chart.series(rc, p, best, n, Theme.BEST, true, 2.2f);
    chart.frontier(
        rc,
        p,
        n - 1,
        new float[][] {{best[n - 1]}, {mean[n - 1]}, {div[n - 1]}},
        new Color[] {Theme.BEST, Theme.MEAN, Theme.DIVERSITY});
  }

  private void drawTeamToggle(float rightX, float midY) {
    Draw d = rc.draw;
    String herb = "Herbivore";
    String carn = "Carnivore";
    float h = 22f;
    float segH = rc.fonts.width(rc.fonts.sansButton, herb) + 20f;
    float segC = rc.fonts.width(rc.fonts.sansButton, carn) + 20f;
    float total = segH + segC;
    float x = rightX - total;
    float y0 = midY - h / 2f;
    boolean herbActive = runs.team() == Team.HERBIVORE;
    d.roundedRect(x, y0, total, h, 6f, Theme.PANEL_ALT);
    d.strokeRoundedRect(x, y0, total, h, 6f, Theme.BORDER_MUTED);
    if (herbActive) {
      d.roundedRect(x + 2f, y0 + 2f, segH - 2f, h - 4f, 5f, Theme.HERBIVORE);
    } else {
      d.roundedRect(x + segH, y0 + 2f, segC - 2f, h - 4f, 5f, Theme.CARNIVORE);
    }
    d.textMid(
        rc.fonts.sansButton, herb, x + 10f, midY, herbActive ? Theme.FIELD : Theme.TEXT_MUTED);
    d.textMid(
        rc.fonts.sansButton,
        carn,
        x + segH + 10f,
        midY,
        herbActive ? Theme.TEXT_MUTED : Theme.FIELD);
  }

  private void drawChampion(float x, float y, float w, float h) {
    Draw d = rc.draw;
    d.rect(x, y, w, h, Theme.PANEL);
    d.line(x, y, x, y + h, Theme.HAIRLINE);
    float pad = Theme.PANEL_PAD;
    float top = y + h - pad;
    d.tracked(rc.fonts.mono10, "CHAMPION BRAIN", x + pad, top - 2f, 1.4f, Theme.TEXT_MUTED);

    Color teamColor = runs.team() == Team.CARNIVORE ? Theme.CARNIVORE : Theme.HERBIVORE;
    float cursor = top - 26f;
    List<GenerationRecordView.Stats> pts = runs.teamPoints(runs.selectedRunId());
    float bestFit = pts.isEmpty() ? 0f : (float) pts.get(pts.size() - 1).bestFitness();
    GenomeView g = runs.champion().orElse(null);

    cursor =
        champStat(
            x + pad,
            x + w - pad,
            cursor,
            "team",
            runs.team() == Team.CARNIVORE ? "carnivore" : "herbivore",
            teamColor);
    cursor =
        champStat(
            x + pad,
            x + w - pad,
            cursor,
            "best fitness",
            String.format("%.3f", bestFit),
            Theme.BEST);
    int nodes = g == null ? 0 : g.nodes().size();
    int conns = g == null ? 0 : g.connections().size();
    cursor =
        champStat(x + pad, x + w - pad, cursor, "nodes", String.valueOf(nodes), Theme.TEXT_BODY);
    cursor =
        champStat(x + pad, x + w - pad, cursor, "conns", String.valueOf(conns), Theme.TEXT_BODY);

    float btnH = 30f;
    float netTop = cursor - 8f;
    float netBottom = y + pad + btnH + 12f;
    if (g != null) {
      net.layout(g, x + pad, netBottom, w - pad * 2, netTop - netBottom, false);
      net.draw(rc, g, 5f, false, true, -1);
    } else {
      d.textCentered(
          rc.fonts.mono11, "no champion", x + w / 2f, (netTop + netBottom) / 2f, Theme.TEXT_FAINT);
    }

    float[] btn = exportButtonRect();
    d.button(
        btn[0],
        btn[1],
        btn[2],
        btn[3],
        "Export genome",
        rc.fonts.sansButton,
        null,
        Theme.BORDER_MUTED,
        Theme.TEXT_BODY);
  }

  private float champStat(float left, float right, float y, String label, String val, Color c) {
    rc.draw.textMid(rc.fonts.mono11, label, left, y, Theme.TEXT_FAINT);
    rc.draw.textMidRight(rc.fonts.mono12, val, right, y, c);
    return y - 20f;
  }

  private void drawEmpty(float x, float y, float w, float h) {
    Draw d = rc.draw;
    float cx = x + w / 2f;
    float cy = y + h / 2f;
    d.textCentered(rc.fonts.sansTitle, "No runs yet", cx, cy + 40f, Theme.TEXT_PRIMARY);
    d.textCentered(
        rc.fonts.mono12,
        "Launch a co-evolution run to see fitness curves.",
        cx,
        cy + 14f,
        Theme.TEXT_MUTED);
    float bw = 200f;
    d.button(
        cx - bw / 2f,
        cy - 36f,
        bw,
        36f,
        "Start your first run",
        rc.fonts.sansButton,
        Theme.ACCENT,
        null,
        Theme.FIELD);
  }

  private static String runLabel(RunSummary run) {
    if (run == null) {
      return "run —";
    }
    return run.id() < 1000 ? String.format("run %03d", run.id()) : "run " + run.id();
  }

  private static String seed(long seed) {
    return seed < 10000 ? String.format("%04d", seed) : String.valueOf(seed);
  }
}
