package dev.maelitop.evolution.client.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.Fonts;
import dev.maelitop.evolution.client.render.RenderContext;
import dev.maelitop.evolution.client.render.Theme;
import java.util.List;

/**
 * Top HUD bar: wordmark + context label on the left, label/value readouts + status on the right.
 */
public final class HudBar {

  public static final float HEIGHT = Theme.HUD_HEIGHT;

  public record Readout(String label, String value, Color valueColor) {}

  public record Status(String text, Color color) {}

  private HudBar() {}

  public static void draw(
      RenderContext rc,
      float width,
      float screenHeight,
      String context,
      List<Readout> readouts,
      Status status) {
    Draw d = rc.draw;
    Fonts f = rc.fonts;
    float top = screenHeight;
    float y0 = screenHeight - HEIGHT;
    float mid = y0 + HEIGHT / 2f;

    d.rect(0, y0, width, HEIGHT, Theme.PANEL);
    d.line(0, y0, width, y0, Theme.HAIRLINE);

    d.diamond(25f, mid, 17f, Theme.ACCENT);
    float x = 44f;
    d.textMid(f.sansWordmark, "GENOME", x, mid, Theme.TEXT_PRIMARY);
    x += f.width(f.sansWordmark, "GENOME") + 12f;
    d.textMid(f.mono12, "/ " + context, x, mid, Theme.TEXT_FAINT_2);

    float right = width - 16f;
    if (status != null) {
      right = drawStatus(rc, right, mid, status) - 22f;
    }
    for (int i = readouts.size() - 1; i >= 0; i--) {
      right = drawReadout(rc, readouts.get(i), right, top, y0) - 22f;
    }
  }

  private static float drawReadout(RenderContext rc, Readout r, float right, float top, float y0) {
    Draw d = rc.draw;
    Fonts f = rc.fonts;
    BitmapFont valueFont = f.mono14;
    BitmapFont labelFont = f.mono10;
    float blockW = Math.max(f.width(labelFont, r.label()), f.width(valueFont, r.value()));
    float left = right - blockW;
    d.textRight(labelFont, r.label(), right, top - 13f, Theme.TEXT_FAINT);
    d.textRight(valueFont, r.value(), right, y0 + 19f, r.valueColor());
    return left;
  }

  private static float drawStatus(RenderContext rc, float right, float mid, Status s) {
    Draw d = rc.draw;
    Fonts f = rc.fonts;
    float h = 24f;
    float dot = 4.5f;
    float padX = 11f;
    float textW = f.width(f.mono12, s.text());
    float w = padX * 2 + dot * 2 + 8f + textW;
    float x = right - w;
    d.roundedRect(x, mid - h / 2f, w, h, 6f, Theme.PANEL_ALT);
    d.strokeRoundedRect(x, mid - h / 2f, w, h, 6f, s.color(), 0.75f);
    d.glowDisc(x + padX + dot, mid, dot, s.color(), 1f);
    d.circle(x + padX + dot, mid, dot, s.color());
    d.textMid(f.mono12, s.text(), x + padX + dot * 2 + 8f, mid, s.color());
    return x;
  }
}
