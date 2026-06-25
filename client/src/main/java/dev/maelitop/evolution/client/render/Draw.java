package dev.maelitop.evolution.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

/**
 * Immediate-mode drawing toolkit over one {@link ShapeRenderer} and one {@link SpriteBatch}. Modes
 * (filled / line / glow / text) are switched lazily so consecutive same-mode draws batch; additive
 * glow is its own pass because GL blend mode applies per shape batch, not per shape.
 */
public final class Draw {

  private enum Mode {
    NONE,
    FILLED,
    LINE,
    GLOW,
    TEXT
  }

  private final ShapeRenderer shapes;
  private final SpriteBatch batch;
  private final Fonts fonts;
  private final Color scratch = new Color();
  private final Matrix4 projection = new Matrix4();

  private Mode mode = Mode.NONE;

  public Draw(ShapeRenderer shapes, SpriteBatch batch, Fonts fonts) {
    this.shapes = shapes;
    this.batch = batch;
    this.fonts = fonts;
  }

  public Fonts fonts() {
    return fonts;
  }

  public float width(BitmapFont font, CharSequence text) {
    return fonts.width(font, text);
  }

  // --- pass / projection management -------------------------------------------------------------

  public void setProjection(Matrix4 m) {
    flush();
    projection.set(m);
    shapes.setProjectionMatrix(projection);
    batch.setProjectionMatrix(projection);
  }

  /** Begins scissor clipping to a logical-space rect (converted to back-buffer pixels). */
  public void clip(float x, float y, float w, float h, float density) {
    flush();
    Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
    Gdx.gl.glScissor(
        Math.round(x * density),
        Math.round(y * density),
        Math.round(w * density),
        Math.round(h * density));
  }

  public void clipEnd() {
    flush();
    Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
  }

  public void flush() {
    switch (mode) {
      case FILLED, LINE, GLOW -> shapes.end();
      case TEXT -> batch.end();
      case NONE -> {}
    }
    if (mode == Mode.GLOW) {
      normalBlend();
    }
    mode = Mode.NONE;
  }

  private void useFilled() {
    if (mode == Mode.FILLED) {
      return;
    }
    leave();
    normalBlend();
    shapes.begin(ShapeType.Filled);
    mode = Mode.FILLED;
  }

  private void useLine() {
    if (mode == Mode.LINE) {
      return;
    }
    leave();
    normalBlend();
    shapes.begin(ShapeType.Line);
    mode = Mode.LINE;
  }

  private void useGlow() {
    if (mode == Mode.GLOW) {
      return;
    }
    leave();
    additiveBlend();
    shapes.begin(ShapeType.Filled);
    mode = Mode.GLOW;
  }

  private void useText() {
    if (mode == Mode.TEXT) {
      return;
    }
    leave();
    batch.begin();
    mode = Mode.TEXT;
  }

  private void leave() {
    switch (mode) {
      case FILLED, LINE -> shapes.end();
      case GLOW -> {
        shapes.end();
        normalBlend();
      }
      case TEXT -> batch.end();
      case NONE -> {}
    }
    mode = Mode.NONE;
  }

  private static void additiveBlend() {
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
  }

  private static void normalBlend() {
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
  }

  // --- fills ------------------------------------------------------------------------------------

  public void rect(float x, float y, float w, float h, Color color) {
    useFilled();
    shapes.setColor(color);
    shapes.rect(x, y, w, h);
  }

  public void rect(float x, float y, float w, float h, Color color, float alpha) {
    useFilled();
    shapes.setColor(scratch.set(color.r, color.g, color.b, alpha));
    shapes.rect(x, y, w, h);
  }

  public void circle(float x, float y, float r, Color color) {
    useFilled();
    shapes.setColor(color);
    shapes.circle(x, y, r, segments(r));
  }

  public void circle(float x, float y, float r, Color color, float alpha) {
    useFilled();
    shapes.setColor(scratch.set(color.r, color.g, color.b, alpha));
    shapes.circle(x, y, r, segments(r));
  }

  public void roundedRect(float x, float y, float w, float h, float r, Color color) {
    roundedRect(x, y, w, h, r, color, color.a);
  }

  public void roundedRect(float x, float y, float w, float h, float r, Color color, float alpha) {
    useFilled();
    shapes.setColor(scratch.set(color.r, color.g, color.b, alpha));
    r = Math.min(r, Math.min(w, h) / 2f);
    shapes.rect(x + r, y, w - 2 * r, h);
    shapes.rect(x, y + r, r, h - 2 * r);
    shapes.rect(x + w - r, y + r, r, h - 2 * r);
    int seg = segments(r);
    shapes.arc(x + r, y + r, r, 180, 90, seg);
    shapes.arc(x + w - r, y + r, r, 270, 90, seg);
    shapes.arc(x + w - r, y + h - r, r, 0, 90, seg);
    shapes.arc(x + r, y + h - r, r, 90, 90, seg);
  }

  // --- strokes ----------------------------------------------------------------------------------

  /** 1px hairline rectangle outline. */
  public void hairline(float x, float y, float w, float h, Color color) {
    useLine();
    shapes.setColor(color);
    shapes.rect(x, y, w, h);
  }

  public void strokeRoundedRect(float x, float y, float w, float h, float r, Color color) {
    strokeRoundedRect(x, y, w, h, r, color, color.a);
  }

  public void strokeRoundedRect(
      float x, float y, float w, float h, float r, Color color, float alpha) {
    useLine();
    shapes.setColor(scratch.set(color.r, color.g, color.b, alpha));
    r = Math.min(r, Math.min(w, h) / 2f);
    shapes.line(x + r, y, x + w - r, y);
    shapes.line(x + r, y + h, x + w - r, y + h);
    shapes.line(x, y + r, x, y + h - r);
    shapes.line(x + w, y + r, x + w, y + h - r);
    arcOutline(x + r, y + r, r, 180);
    arcOutline(x + w - r, y + r, r, 270);
    arcOutline(x + w - r, y + h - r, r, 0);
    arcOutline(x + r, y + h - r, r, 90);
  }

  /** Draws a 90° arc as connected line segments (no center radii, unlike ShapeRenderer.arc). */
  private void arcOutline(float cx, float cy, float r, float startDeg) {
    int steps = Math.max(4, (int) (r / 2f));
    float start = startDeg * MathUtils.degreesToRadians;
    float sweep = 90f * MathUtils.degreesToRadians;
    float px = cx + r * MathUtils.cos(start);
    float py = cy + r * MathUtils.sin(start);
    for (int i = 1; i <= steps; i++) {
      float a = start + sweep * (i / (float) steps);
      float nx = cx + r * MathUtils.cos(a);
      float ny = cy + r * MathUtils.sin(a);
      shapes.line(px, py, nx, ny);
      px = nx;
      py = ny;
    }
  }

  public void line(float x1, float y1, float x2, float y2, Color color) {
    line(x1, y1, x2, y2, color, 1f);
  }

  public void line(float x1, float y1, float x2, float y2, Color color, float alpha) {
    useLine();
    shapes.setColor(scratch.set(color.r, color.g, color.b, alpha));
    shapes.line(x1, y1, x2, y2);
  }

  /** Thick line via a filled quad; widths above ~1px should use this rather than {@link #line}. */
  public void thickLine(float x1, float y1, float x2, float y2, float width, Color color) {
    thickLine(x1, y1, x2, y2, width, color, 1f);
  }

  public void thickLine(
      float x1, float y1, float x2, float y2, float width, Color color, float alpha) {
    useFilled();
    shapes.setColor(scratch.set(color.r, color.g, color.b, alpha));
    shapes.rectLine(x1, y1, x2, y2, width);
  }

  public void strokeCircle(float cx, float cy, float r, float width, Color color, float alpha) {
    arc(cx, cy, r, 0f, 360f, width, color, alpha);
  }

  /** Thick stroked arc, sweeping clockwise from {@code startDeg} measured CCW from +X. */
  public void arc(
      float cx,
      float cy,
      float r,
      float startDeg,
      float sweepDeg,
      float width,
      Color color,
      float alpha) {
    useFilled();
    shapes.setColor(scratch.set(color.r, color.g, color.b, alpha));
    int steps = Math.max(6, segments(r));
    float start = startDeg * MathUtils.degreesToRadians;
    float sweep = sweepDeg * MathUtils.degreesToRadians;
    float prevX = cx + r * MathUtils.cos(start);
    float prevY = cy + r * MathUtils.sin(start);
    for (int i = 1; i <= steps; i++) {
      float a = start + sweep * (i / (float) steps);
      float nx = cx + r * MathUtils.cos(a);
      float ny = cy + r * MathUtils.sin(a);
      shapes.rectLine(prevX, prevY, nx, ny, width);
      prevX = nx;
      prevY = ny;
    }
  }

  public void dashedLine(
      float x1, float y1, float x2, float y2, float dash, float gap, float phase, Color color) {
    useLine();
    shapes.setColor(color);
    float dx = x2 - x1;
    float dy = y2 - y1;
    float len = (float) Math.sqrt(dx * dx + dy * dy);
    if (len < 1e-3f) {
      return;
    }
    float ux = dx / len;
    float uy = dy / len;
    float period = dash + gap;
    float t = -((phase % period) + period) % period;
    while (t < len) {
      float s = Math.max(t, 0f);
      float e = Math.min(t + dash, len);
      if (e > s) {
        shapes.line(x1 + ux * s, y1 + uy * s, x1 + ux * e, y1 + uy * e);
      }
      t += period;
    }
  }

  public void dashedCircle(
      float cx, float cy, float r, float dashDeg, float gapDeg, float phaseDeg, Color color) {
    useLine();
    shapes.setColor(color);
    float period = dashDeg + gapDeg;
    for (float a = phaseDeg; a < phaseDeg + 360f; a += period) {
      float s = a * MathUtils.degreesToRadians;
      float e = (a + dashDeg) * MathUtils.degreesToRadians;
      shapes.line(
          cx + r * MathUtils.cos(s),
          cy + r * MathUtils.sin(s),
          cx + r * MathUtils.cos(e),
          cy + r * MathUtils.sin(e));
    }
  }

  // --- glow (additive pass) ---------------------------------------------------------------------

  /** Soft additive halo around a point. {@code intensity} scales the alpha. */
  public void glowDisc(float x, float y, float r, Color color, float intensity) {
    useGlow();
    shapes.setColor(scratch.set(color.r, color.g, color.b, 0.10f * intensity));
    shapes.circle(x, y, r * 2.1f, segments(r * 2.1f));
    shapes.setColor(scratch.set(color.r, color.g, color.b, 0.16f * intensity));
    shapes.circle(x, y, r * 1.35f, segments(r * 1.35f));
  }

  public void glowLine(float x1, float y1, float x2, float y2, float width, Color color, float a) {
    useGlow();
    shapes.setColor(scratch.set(color.r, color.g, color.b, a));
    shapes.rectLine(x1, y1, x2, y2, width);
  }

  // --- polylines --------------------------------------------------------------------------------

  public void polyline(float[] xs, float[] ys, int n, float width, Color color, float alpha) {
    useFilled();
    shapes.setColor(scratch.set(color.r, color.g, color.b, alpha));
    for (int i = 0; i < n - 1; i++) {
      shapes.rectLine(xs[i], ys[i], xs[i + 1], ys[i + 1], width);
      shapes.circle(xs[i + 1], ys[i + 1], width / 2f, 8);
    }
  }

  public void glowPolyline(float[] xs, float[] ys, int n, float width, Color color, float alpha) {
    useGlow();
    shapes.setColor(scratch.set(color.r, color.g, color.b, alpha));
    for (int i = 0; i < n - 1; i++) {
      shapes.rectLine(xs[i], ys[i], xs[i + 1], ys[i + 1], width);
    }
  }

  // --- chrome -----------------------------------------------------------------------------------

  /** A rotated-square diamond with a top-light gradient and a soft accent glow (wordmark logo). */
  public void diamond(float cx, float cy, float size, Color color) {
    float h = size / 2f;
    useGlow();
    shapes.setColor(scratch.set(Theme.ACCENT.r, Theme.ACCENT.g, Theme.ACCENT.b, 0.16f));
    shapes.circle(cx, cy, size * 0.95f, 24);
    useFilled();
    float topY = cy + h;
    float botY = cy - h;
    shapes.triangle(cx, topY, cx + h, cy, cx, botY, Theme.ACCENT_BRIGHT, color, Theme.ACCENT_DEEP);
    shapes.triangle(cx, topY, cx - h, cy, cx, botY, Theme.ACCENT_BRIGHT, color, Theme.ACCENT_DEEP);
  }

  /** Draws a key-cap chip at (x,y) bottom-left; returns its width. */
  public float keyCap(float x, float y, String label) {
    return keyCap(x, y, label, Theme.TEXT_BODY_DIM, Theme.KEYCAP_BORDER);
  }

  public static final float KEYCAP_H = 20f;

  public float keyCap(float x, float y, String label, Color textColor, Color border) {
    float padX = 7f;
    float w = fonts.width(fonts.mono11, label) + padX * 2;
    float h = KEYCAP_H;
    roundedRect(x, y, w, h, 4f, Theme.KEYCAP_FILL);
    strokeRoundedRect(x, y, w, h, 4f, border);
    line(x + 3, y + 1.5f, x + w - 3, y + 1.5f, border);
    textMid(fonts.mono11, label, x + padX, y + h / 2f, textColor);
    return w;
  }

  /** A rounded-rect button with optional fill/border and a centered label. */
  public void button(
      float x,
      float y,
      float w,
      float h,
      String label,
      BitmapFont font,
      Color fill,
      Color border,
      Color textColor) {
    if (fill != null) {
      roundedRect(x, y, w, h, Theme.RADIUS - 1, fill);
    }
    if (border != null) {
      strokeRoundedRect(x, y, w, h, Theme.RADIUS - 1, border);
    }
    text(
        font,
        label,
        x + (w - fonts.width(font, label)) / 2f,
        y + h / 2f + font.getCapHeight() / 2f,
        textColor);
  }

  /** Rounded pill with optional fill and border; returns its width. */
  public float pill(
      float x,
      float y,
      float h,
      String label,
      BitmapFont font,
      Color fill,
      Color border,
      Color textColor) {
    float padX = 9f;
    float w = fonts.width(font, label) + padX * 2;
    float r = h / 2f;
    if (fill != null) {
      roundedRect(x, y, w, h, r, fill);
    }
    if (border != null) {
      strokeRoundedRect(x, y, w, h, r, border);
    }
    text(font, label, x + padX, y + h - (h - fontCapHeight(font)) / 2f, textColor);
    return w;
  }

  // --- text -------------------------------------------------------------------------------------

  /** Draws text with (x, baselineTopY) where y is the top of the cap line (libGDX top-left). */
  public void text(BitmapFont font, CharSequence s, float x, float y, Color color) {
    useText();
    font.setColor(color);
    font.draw(batch, s, x, y);
  }

  public void text(BitmapFont font, CharSequence s, float x, float y, Color color, float alpha) {
    useText();
    font.setColor(scratch.set(color.r, color.g, color.b, alpha));
    font.draw(batch, s, x, y);
  }

  public void textRight(BitmapFont font, CharSequence s, float xRight, float y, Color color) {
    text(font, s, xRight - fonts.width(font, s), y, color);
  }

  public void textCentered(BitmapFont font, CharSequence s, float cx, float y, Color color) {
    text(font, s, cx - fonts.width(font, s) / 2f, y, color);
  }

  /** Draws text vertically centered on {@code midY} at left edge {@code x}. */
  public void textMid(BitmapFont font, CharSequence s, float x, float midY, Color color) {
    text(font, s, x, midY + font.getCapHeight() / 2f, color);
  }

  public void textMidRight(BitmapFont font, CharSequence s, float xRight, float midY, Color color) {
    text(font, s, xRight - fonts.width(font, s), midY + font.getCapHeight() / 2f, color);
  }

  /** Uppercase-style tracked text (letter-spacing). Returns advance width. */
  public float tracked(BitmapFont font, String s, float x, float y, float spacing, Color color) {
    useText();
    font.setColor(color);
    float cx = x;
    for (int i = 0; i < s.length(); i++) {
      String ch = s.substring(i, i + 1);
      font.draw(batch, ch, cx, y);
      cx += fonts.width(font, ch) + spacing;
    }
    return cx - x;
  }

  private float fontCapHeight(BitmapFont font) {
    return font.getCapHeight();
  }

  private static int segments(float r) {
    return Math.max(12, Math.min(96, (int) (r * 1.5f)));
  }
}
