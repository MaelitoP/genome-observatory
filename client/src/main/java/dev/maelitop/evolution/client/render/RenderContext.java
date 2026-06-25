package dev.maelitop.evolution.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Shared rendering resources for every screen: one ShapeRenderer, SpriteBatch, font set and Draw.
 */
public final class RenderContext {

  public final ShapeRenderer shapes;
  public final SpriteBatch batch;
  public final Fonts fonts;
  public final Draw draw;

  private final Matrix4 screen = new Matrix4();

  private float time;

  public RenderContext() {
    shapes = new ShapeRenderer();
    batch = new SpriteBatch();
    fonts = new Fonts(detectDensity());
    draw = new Draw(shapes, batch, fonts);
    resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
  }

  public void resize(int width, int height) {
    screen.setToOrtho2D(0, 0, width, height);
  }

  public Matrix4 screenProjection() {
    return screen;
  }

  public float density() {
    return fonts.density();
  }

  public float time() {
    return time;
  }

  /** Clears to the app background and prepares blend state for the frame. */
  public void beginFrame() {
    time += Gdx.graphics.getDeltaTime();
    ScreenUtils.clear(Theme.APP_BG);
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    draw.setProjection(screen);
  }

  private static float detectDensity() {
    int logical = Math.max(1, Gdx.graphics.getWidth());
    float ratio = Gdx.graphics.getBackBufferWidth() / (float) logical;
    return Math.max(1f, Math.min(3f, ratio));
  }

  public void dispose() {
    shapes.dispose();
    batch.dispose();
    fonts.dispose();
  }
}
