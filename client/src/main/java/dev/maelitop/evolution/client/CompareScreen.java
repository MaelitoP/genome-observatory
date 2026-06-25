package dev.maelitop.evolution.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.Theme;
import dev.maelitop.evolution.client.view.FitnessChart;
import dev.maelitop.evolution.client.view.HintBar;
import dev.maelitop.evolution.client.view.HudBar;
import java.util.ArrayList;
import java.util.List;

final class CompareScreen extends ObservatoryScreen {

  private static final Color[] PALETTE = {
    Theme.ACCENT, Theme.DIVERSITY, Theme.BEST, Theme.CARNIVORE, Theme.MEAN
  };

  private final RunBrowser runs;
  private final FitnessChart chart = new FitnessChart();
  private final float[] buf = new float[4096];

  CompareScreen(EvolutionClient app, RunBrowser runs) {
    super(app);
    this.runs = runs;
  }

  @Override
  public void show() {
    super.show();
    runs.ensureRunsLoaded();
  }

  @Override
  protected void input(float delta) {
    if (Gdx.input.isKeyJustPressed(Keys.C)) {
      runs.toggleCompareSelected();
    }
    if (Gdx.input.isKeyJustPressed(Keys.X)) {
      runs.clearCompare();
    }
  }

  @Override
  protected void drawBody(float delta) {
    runs.sync();
    float tableW = Theme.METRICS_W;
    float chartW = bodyW - tableW;
    List<Long> ids = new ArrayList<>(runs.compared());

    if (ids.isEmpty()) {
      drawEmpty(bodyX, bodyY, chartW, bodyH);
    } else {
      drawChart(bodyX, bodyY, chartW, bodyH, ids);
    }
    drawTable(bodyW - tableW, bodyY, tableW, bodyH, ids);
  }

  private void drawChart(float x, float y, float w, float h, List<Long> ids) {
    Draw d = rc.draw;
    float pad = Theme.PANEL_PAD;
    d.text(
        rc.fonts.sansTitle, "Best fitness — comparison", x + pad, y + h - pad, Theme.TEXT_PRIMARY);
    d.text(
        rc.fonts.mono11,
        runs.team() == dev.maelitop.evolution.protocol.Team.CARNIVORE ? "carnivore" : "herbivore",
        x + pad,
        y + h - pad - 18f,
        Theme.TEXT_MUTED);

    int maxGen = 2;
    float bestMax = 1e-6f;
    for (long id : ids) {
      List<GenerationRecordView.Stats> pts = runs.bestPoints(id, runs.team());
      maxGen = Math.max(maxGen, pts.size());
      for (GenerationRecordView.Stats s : pts) {
        bestMax = Math.max(bestMax, (float) s.bestFitness());
      }
    }
    bestMax *= 1.08f;
    FitnessChart.Plot p =
        chart.frame(rc, x + pad, y + pad, w - pad * 2, h - pad * 2 - 30f, maxGen, 1f);

    float ly = y + h - pad - 40f;
    float lx = x + pad;
    for (int i = 0; i < ids.size(); i++) {
      long id = ids.get(i);
      Color col = PALETTE[i % PALETTE.length];
      List<GenerationRecordView.Stats> pts = runs.bestPoints(id, runs.team());
      int n = pts.size();
      for (int j = 0; j < n; j++) {
        buf[j] = (float) pts.get(j).bestFitness() / bestMax;
      }
      boolean emphasis = id == runs.selectedRunId();
      chart.series(rc, p, buf, n, col, emphasis, emphasis ? 2.6f : 1.8f);
      if (n >= 1) {
        d.glowDisc(p.px(n - 1), p.py(buf[n - 1]), 3f, col, 1f);
        d.circle(p.px(n - 1), p.py(buf[n - 1]), 2.4f, col);
      }
      lx = legendEntry(d, lx, ly, col, id);
    }
  }

  private float legendEntry(Draw d, float x, float y, Color col, long id) {
    d.thickLine(x, y, x + 16f, y, 2.4f, col, 1f);
    x += 22f;
    String label = label(id);
    d.textMid(rc.fonts.mono11, label, x, y, Theme.TEXT_BODY);
    return x + rc.fonts.width(rc.fonts.mono11, label) + 18f;
  }

  private void drawTable(float x, float y, float w, float h, List<Long> ids) {
    Draw d = rc.draw;
    d.rect(x, y, w, h, Theme.PANEL);
    d.line(x, y, x, y + h, Theme.HAIRLINE);
    float pad = Theme.PANEL_PAD;
    d.tracked(rc.fonts.mono10, "FINAL METRICS", x + pad, y + h - pad - 2f, 1.4f, Theme.TEXT_MUTED);

    float cardH = 92f;
    float cardTop = y + h - pad - 26f;
    long winner = -1;
    float winnerBest = -1f;
    for (int i = 0; i < ids.size(); i++) {
      long id = ids.get(i);
      Color col = PALETTE[i % PALETTE.length];
      float cardY = cardTop - cardH - i * (cardH + 10f);
      drawCard(d, x + pad, cardY, w - pad * 2, cardH, id, col);
      List<GenerationRecordView.Stats> pts = runs.bestPoints(id, runs.team());
      float b = pts.isEmpty() ? 0 : (float) pts.get(pts.size() - 1).bestFitness();
      if (b > winnerBest) {
        winnerBest = b;
        winner = id;
      }
    }
    if (winner >= 0) {
      d.text(rc.fonts.mono11, "winner: " + label(winner), x + pad, y + pad + 2f, Theme.ACCENT);
    }
  }

  private void drawCard(Draw d, float x, float y, float w, float h, long id, Color col) {
    d.roundedRect(x, y, w, h, 5f, Theme.PANEL_ALT);
    d.strokeRoundedRect(x, y, w, h, 5f, Theme.HAIRLINE);
    d.rect(x, y, 2.5f, h, col);
    RunSummary run = findRun(id);
    List<GenerationRecordView.Stats> pts = runs.bestPoints(id, runs.team());
    GenerationRecordView.Stats last = pts.isEmpty() ? null : pts.get(pts.size() - 1);
    float pad = 10f;
    d.textMid(rc.fonts.mono13, label(id), x + pad, y + h - 14f, col);
    String tag = run != null && run.carnivores() > 0 ? "CO-EVO" : "herb-only";
    d.textMidRight(
        rc.fonts.mono10,
        tag,
        x + w - pad,
        y + h - 14f,
        run != null && run.carnivores() > 0 ? Theme.CARNIVORE : Theme.TEXT_FAINT_3);

    float row = y + h - 36f;
    metric(d, x + pad, row, "best", last == null ? "—" : f3(last.bestFitness()), Theme.BEST);
    metric(d, x + pad + 100f, row, "mean", last == null ? "—" : f3(last.meanFitness()), Theme.MEAN);
    row -= 18f;
    metric(d, x + pad, row, "div", last == null ? "—" : f3(last.diversity()), Theme.DIVERSITY);
    metric(
        d,
        x + pad + 100f,
        row,
        "gens",
        last == null ? "—" : String.valueOf(pts.size()),
        Theme.TEXT_BODY);
  }

  private void metric(Draw d, float x, float y, String label, String val, Color c) {
    d.textMid(rc.fonts.mono10, label, x, y, Theme.TEXT_FAINT);
    d.textMid(rc.fonts.mono12, val, x + 34f, y, c);
  }

  private void drawEmpty(float x, float y, float w, float h) {
    Draw d = rc.draw;
    d.textCentered(
        rc.fonts.sansTitle, "Nothing to compare", x + w / 2f, y + h / 2f + 16f, Theme.TEXT_PRIMARY);
    d.textCentered(
        rc.fonts.mono12,
        "press C to add the selected run",
        x + w / 2f,
        y + h / 2f - 10f,
        Theme.TEXT_MUTED);
  }

  @Override
  protected void drawChrome() {
    HudBar.draw(rc, width, height, "compare", List.of(), null);
    HintBar.draw(
        rc,
        width,
        List.of(new HintBar.Hint("C", "add/remove run"), new HintBar.Hint("X", "clear all")),
        new HintBar.Hint("TAB", "world"));
  }

  private RunSummary findRun(long id) {
    for (RunSummary r : runs.runs()) {
      if (r.id() == id) {
        return r;
      }
    }
    return null;
  }

  private String label(long id) {
    RunSummary run = findRun(id);
    String idText = id < 1000 ? String.format("run %03d", id) : "run " + id;
    if (run == null) {
      return idText;
    }
    String seed =
        run.seed() < 10000 ? String.format("%04d", run.seed()) : String.valueOf(run.seed());
    return idText + " · seed " + seed;
  }

  private static String f3(double v) {
    return String.format("%.3f", v);
  }
}
