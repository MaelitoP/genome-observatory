package dev.maelitop.evolution.client;

import com.badlogic.gdx.Gdx;
import dev.maelitop.evolution.client.GenomeView.ConnectionView;
import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.Theme;
import dev.maelitop.evolution.client.view.HintBar;
import dev.maelitop.evolution.client.view.HudBar;
import dev.maelitop.evolution.client.view.NetworkRenderer;
import dev.maelitop.evolution.protocol.Team;
import java.util.List;

final class NetworkScreen extends ObservatoryScreen {

  private final RunBrowser runs;
  private final NetworkRenderer net = new NetworkRenderer();
  private int hoverId = -1;

  NetworkScreen(EvolutionClient app, RunBrowser runs) {
    super(app);
    this.runs = runs;
  }

  @Override
  public void show() {
    super.show();
    runs.ensureRunsLoaded();
  }

  @Override
  protected void drawBody(float delta) {
    runs.sync();
    Draw d = rc.draw;
    d.rect(bodyX, bodyY, bodyW, bodyH, Theme.NETWORK_BG);

    GenomeView g = runs.champion().orElse(null);
    if (g == null) {
      d.textCentered(
          rc.fonts.mono12,
          "no champion loaded — select a run in analytics",
          bodyX + bodyW / 2f,
          bodyY + bodyH / 2f,
          Theme.TEXT_FAINT);
      return;
    }

    float capY = bodyY + bodyH - 18f;
    d.tracked(rc.fonts.mono10, "INPUTS", bodyX + 40f, capY, 1.4f, Theme.NODE_INPUT);
    d.tracked(rc.fonts.mono10, "HIDDEN", bodyX + bodyW * 0.5f - 32f, capY, 1.4f, Theme.TEXT_MUTED);
    d.tracked(rc.fonts.mono10, "OUTPUTS", bodyX + bodyW - 110f, capY, 1.4f, Theme.ACCENT);

    net.layout(g, bodyX, bodyY, bodyW, bodyH - 28f, true);
    float mx = Gdx.input.getX();
    float my = height - Gdx.input.getY();
    hoverId = net.pickNode(mx, my, 12f);
    net.draw(rc, g, 7f, true, true, hoverId);
  }

  @Override
  protected void drawChrome() {
    GenomeView g = runs.champion().orElse(null);
    int nodes = g == null ? 0 : g.nodes().size();
    int conns =
        g == null ? 0 : (int) g.connections().stream().filter(ConnectionView::enabled).count();
    String teamLabel = runs.team() == Team.CARNIVORE ? "CARN" : "HERB";
    HudBar.draw(
        rc,
        width,
        height,
        "network",
        List.of(
            new HudBar.Readout("RUN", runLabel(), Theme.ACCENT_BRIGHT),
            new HudBar.Readout(
                "TEAM",
                teamLabel,
                runs.team() == Team.CARNIVORE ? Theme.CARNIVORE : Theme.HERBIVORE),
            new HudBar.Readout("NODES", String.valueOf(nodes), Theme.TEXT_PRIMARY),
            new HudBar.Readout("CONNS", String.valueOf(conns), Theme.TEXT_PRIMARY)),
        null);
    drawLegendBar();
  }

  private void drawLegendBar() {
    Draw d = rc.draw;
    d.rect(0, 0, width, HintBar.HEIGHT, Theme.PANEL);
    d.line(0, HintBar.HEIGHT, width, HintBar.HEIGHT, Theme.HAIRLINE);
    float mid = HintBar.HEIGHT / 2f;
    float x = 16f;
    x = legendItem(d, x, mid, Theme.WEIGHT_POS, false, "+ weight");
    x = legendItem(d, x, mid, Theme.WEIGHT_NEG, false, "− weight");
    x = legendItem(d, x, mid, Theme.EDGE_DISABLED, true, "disabled");
    d.textMid(rc.fonts.mono11, "thickness = |magnitude|", x, mid, Theme.TEXT_FAINT);

    float descW = rc.fonts.width(rc.fonts.mono11, "compare");
    float capW = rc.fonts.width(rc.fonts.mono11, "TAB") + 12f;
    float bx = width - 16f - (capW + 7f + descW);
    float used = d.keyCap(bx, mid - Draw.KEYCAP_H / 2f, "TAB", Theme.ACCENT_BRIGHT, Theme.ACCENT);
    d.textMid(rc.fonts.mono11, "compare", bx + used + 7f, mid, Theme.ACCENT);
    float ex = bx - 16f - (rc.fonts.width(rc.fonts.mono11, "export") + 24f + 7f);
    float eUsed = d.keyCap(ex, mid - Draw.KEYCAP_H / 2f, "E");
    d.textMid(rc.fonts.mono11, "export", ex + eUsed + 7f, mid, Theme.TEXT_MUTED);
  }

  private float legendItem(
      Draw d, float x, float mid, com.badlogic.gdx.graphics.Color c, boolean dashed, String label) {
    if (dashed) {
      d.dashedLine(x, mid, x + 18f, mid, 4f, 3f, 0f, c);
    } else {
      d.thickLine(x, mid, x + 18f, mid, 2.2f, c, 1f);
    }
    x += 24f;
    d.textMid(rc.fonts.mono11, label, x, mid, Theme.TEXT_MUTED);
    return x + rc.fonts.width(rc.fonts.mono11, label) + 20f;
  }

  private String runLabel() {
    return runs.selectedRun()
        .map(run -> run.id() < 1000 ? String.format("%03d", run.id()) : String.valueOf(run.id()))
        .orElse("—");
  }
}
