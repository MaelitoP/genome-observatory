package dev.maelitop.evolution.client.view;

import com.badlogic.gdx.graphics.Color;
import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.RenderContext;
import dev.maelitop.evolution.client.render.Theme;

/** Axis/grid system and series plotting shared by the analytics and comparison charts. */
public final class FitnessChart {

  private static final float PAD_LEFT = 40f;
  private static final float PAD_BOTTOM = 26f;
  private static final float PAD_TOP = 10f;
  private static final float PAD_RIGHT = 14f;

  public record Plot(float x, float y, float w, float h, int gens, float maxY) {
    public float px(int gen) {
      return gens <= 1 ? x : x + (gen / (float) (gens - 1)) * w;
    }

    public float py(float value) {
      return y + (value / maxY) * h;
    }
  }

  private final float[] xs = new float[4096];
  private final float[] ys = new float[4096];

  /** Draws the grid and axis labels; returns the inner plot rect. */
  public Plot frame(RenderContext rc, float x, float y, float w, float h, int gens, float maxY) {
    Draw d = rc.draw;
    Plot p =
        new Plot(
            x + PAD_LEFT,
            y + PAD_BOTTOM,
            w - PAD_LEFT - PAD_RIGHT,
            h - PAD_BOTTOM - PAD_TOP,
            gens,
            maxY);
    for (int i = 0; i <= 4; i++) {
      float frac = i / 4f;
      float gy = p.y + frac * p.h;
      d.line(p.x, gy, p.x + p.w, gy, Theme.CHART_GRID);
      d.textMidRight(rc.fonts.mono10, fmt(maxY * frac), p.x - 8f, gy, Theme.TEXT_FAINT_3);
    }
    int stepGen = Math.max(1, gens / 6);
    for (int g = 0; g < gens; g += stepGen) {
      d.textCentered(rc.fonts.mono10, String.valueOf(g), p.px(g), y + 8f, Theme.TEXT_FAINT_3);
    }
    d.text(rc.fonts.mono10, "generation →", p.x, y + 8f - 14f, Theme.TEXT_FAINT_2);
    return p;
  }

  public void series(
      RenderContext rc, Plot p, float[] values, int n, Color color, boolean glow, float thickness) {
    if (n < 2) {
      return;
    }
    for (int i = 0; i < n; i++) {
      xs[i] = p.px(i);
      ys[i] = p.py(values[i]);
    }
    Draw d = rc.draw;
    if (glow) {
      d.glowPolyline(xs, ys, n, thickness + 3f, color, 0.18f);
    }
    d.polyline(xs, ys, n, thickness, color, 0.95f);
  }

  /** Dashed vertical frontier at the latest generation, with a pulsing dot on each series value. */
  public void frontier(
      RenderContext rc, Plot p, int gen, float[][] valuesAtFrontier, Color[] cols) {
    Draw d = rc.draw;
    float fx = p.px(gen);
    d.dashedLine(fx, p.y, fx, p.y + p.h, 5f, 5f, rc.time() * 18f, Theme.ACCENT);
    float pulse = 2.6f + 1.2f * (float) Math.sin(rc.time() * 4f);
    for (int i = 0; i < valuesAtFrontier.length; i++) {
      float vy = p.py(valuesAtFrontier[i][0]);
      d.glowDisc(fx, vy, pulse, cols[i], 1f);
      d.circle(fx, vy, 2.4f, cols[i]);
    }
  }

  public void legend(RenderContext rc, float x, float y, String[] labels, Color[] colors) {
    Draw d = rc.draw;
    float cx = x;
    for (int i = 0; i < labels.length; i++) {
      d.thickLine(cx, y, cx + 16f, y, 2.4f, colors[i], 1f);
      cx += 22f;
      d.textMid(rc.fonts.mono11, labels[i], cx, y, Theme.TEXT_BODY);
      cx += rc.fonts.width(rc.fonts.mono11, labels[i]) + 18f;
    }
  }

  private static String fmt(float v) {
    return String.format("%.2f", v);
  }
}
