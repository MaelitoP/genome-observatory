package dev.maelitop.evolution.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import dev.maelitop.evolution.client.render.RenderContext;
import dev.maelitop.evolution.client.view.HintBar;
import dev.maelitop.evolution.client.view.HudBar;

/** Base for every full-screen view: shared frame layout, TAB navigation and chrome ordering. */
abstract class ObservatoryScreen implements Screen {

  protected final EvolutionClient app;
  protected final RenderContext rc;

  protected float width;
  protected float height;
  protected float bodyX;
  protected float bodyY;
  protected float bodyW;
  protected float bodyH;

  protected ObservatoryScreen(EvolutionClient app) {
    this.app = app;
    this.rc = app.context();
  }

  @Override
  public void render(float delta) {
    width = Gdx.graphics.getWidth();
    height = Gdx.graphics.getHeight();
    bodyX = 0;
    bodyY = HintBar.HEIGHT;
    bodyW = width;
    bodyH = height - HudBar.HEIGHT - HintBar.HEIGHT;

    if (!modalOpen() && Gdx.input.isKeyJustPressed(Keys.TAB)) {
      if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)) {
        app.cyclePrev();
      } else {
        app.cycleNext();
      }
      return;
    }

    input(delta);
    rc.beginFrame();
    drawBody(delta);
    rc.draw.setProjection(rc.screenProjection());
    drawChrome();
    rc.draw.flush();
  }

  /** Poll input for this screen. */
  protected void input(float delta) {}

  /** Draw the body region (between HUD and hint bar). May set its own projection. */
  protected abstract void drawBody(float delta);

  /** Draw the top HUD and bottom hint bar (screen-space projection is already set). */
  protected abstract void drawChrome();

  /** When true, TAB does not navigate (a modal owns the keyboard). */
  protected boolean modalOpen() {
    return false;
  }

  @Override
  public void show() {
    Gdx.input.setInputProcessor(null);
  }

  @Override
  public void resize(int w, int h) {
    rc.resize(w, h);
  }

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  @Override
  public void hide() {}

  @Override
  public void dispose() {}
}
