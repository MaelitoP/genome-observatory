package dev.maelitop.evolution.client.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.RenderContext;
import dev.maelitop.evolution.client.render.Theme;
import dev.maelitop.evolution.protocol.CreatureSnapshot;
import dev.maelitop.evolution.protocol.Team;

/** Right-hand world inspector: the selected agent's stats, straight from the world snapshot. */
public final class AgentInspector {

  private final Color teamCol = new Color();

  public void draw(RenderContext rc, float x, float y, float w, float h, CreatureSnapshot agent) {
    Draw d = rc.draw;
    float pad = Theme.PANEL_PAD;
    d.rect(x, y, w, h, Theme.PANEL);
    d.line(x, y, x, y + h, Theme.HAIRLINE);

    float left = x + pad;
    float right = x + w - pad;
    float cursor = y + h - pad - 4f;

    if (agent == null) {
      d.text(rc.fonts.mono12, "no agent selected", left, cursor, Theme.TEXT_FAINT);
      d.text(rc.fonts.mono11, "click a creature", left, cursor - 18f, Theme.TEXT_FAINT_3);
      return;
    }

    teamCol.set(agent.color().r(), agent.color().g(), agent.color().b(), 1f);
    d.text(rc.fonts.mono12, "AGENT ", left, cursor, Theme.TEXT_MUTED);
    float idX = left + rc.fonts.width(rc.fonts.mono12, "AGENT ");
    d.text(rc.fonts.mono14, "#" + agent.id(), idX, cursor + 1f, Theme.ACCENT_BRIGHT);
    teamPill(rc, right, cursor - 2f, agent.team(), teamCol);
    cursor -= 28f;

    cursor = stat(rc, left, right, cursor, "generation", String.valueOf(agent.generation()));
    cursor -= 4f;
    energyRow(rc, left, right, cursor, (float) agent.energy(), teamCol);
    cursor -= 28f;
    cursor = stat(rc, left, right, cursor, "age", fmt(agent.age()));
    cursor = stat(rc, left, right, cursor, "spike", fmt(agent.spike()));
    stat(rc, left, right, cursor, "heading", Math.round(norm(agent.angle())) + "°");
  }

  private float stat(RenderContext rc, float left, float right, float y, String label, String val) {
    rc.draw.textMid(rc.fonts.mono11, label, left, y, Theme.TEXT_FAINT);
    rc.draw.textMidRight(rc.fonts.mono12, val, right, y, Theme.TEXT_BODY);
    return y - 20f;
  }

  private void energyRow(
      RenderContext rc, float left, float right, float y, float energy, Color c) {
    Draw d = rc.draw;
    float e = MathUtils.clamp(energy, 0f, 1f);
    d.textMid(rc.fonts.mono11, "energy", left, y + 6f, Theme.TEXT_FAINT);
    d.textMidRight(rc.fonts.mono12, Math.round(e * 100f) + "%", right, y + 6f, Theme.TEXT_BODY);
    float barY = y - 6f;
    float barW = right - left;
    d.roundedRect(left, barY, barW, 4f, 2f, Theme.PANEL_ALT);
    if (e > 0) {
      d.roundedRect(left, barY, Math.max(e * barW, 4f), 4f, 2f, c);
    }
  }

  private void teamPill(RenderContext rc, float right, float midY, Team team, Color color) {
    Draw d = rc.draw;
    String label = team == Team.CARNIVORE ? "carnivore" : "herbivore";
    float w = rc.fonts.width(rc.fonts.mono10, label) + 16f;
    float pillH = 16f;
    float x = right - w;
    d.roundedRect(x, midY - pillH / 2f, w, pillH, pillH / 2f, color, 0.14f);
    d.strokeRoundedRect(x, midY - pillH / 2f, w, pillH, pillH / 2f, color, 0.7f);
    d.textMid(rc.fonts.mono10, label, x + 8f, midY, color);
  }

  private static double norm(double deg) {
    double d = deg % 360;
    return d < 0 ? d + 360 : d;
  }

  private static String fmt(double v) {
    return String.format("%.1f", v);
  }
}
