package dev.maelitop.evolution.client.preview;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;
import dev.maelitop.evolution.client.CaptureHarness;
import dev.maelitop.evolution.client.render.Theme;
import java.io.File;

/** Drives the real client offscreen against a running server and captures each view to a PNG. */
public final class CaptureMain implements ApplicationListener {

  private final CaptureHarness harness;
  private final String out;
  private final float[] times = {5.0f, 7.0f, 8.4f, 10.4f, 12.2f};
  private final String[] names = {"world", "analytics", "newrun", "network", "compare"};

  private long startNs;
  private int shot;

  private CaptureMain(String out) {
    this.out = out;
    this.harness = new CaptureHarness();
  }

  public static void main(String[] args) {
    String out = args.length > 0 ? args[0] : "build/capture";
    new File(out).mkdirs();
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setWindowedMode(1280, 720);
    config.setInitialVisible(false);
    config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
    config.setInitialBackgroundColor(Theme.APP_BG);
    new Lwjgl3Application(new CaptureMain(out), config);
  }

  @Override
  public void create() {
    harness.create();
    startNs = System.nanoTime();
  }

  @Override
  public void render() {
    harness.render();
    float t = (System.nanoTime() - startNs) / 1e9f;
    if (shot < times.length && t >= times[shot]) {
      capture(names[shot] + ".png");
      shot++;
      if (shot >= times.length) {
        Gdx.app.exit();
        return;
      }
      switch (names[shot]) {
        case "newrun" -> harness.openNewRun();
        case "network" -> {
          harness.closeNewRun();
          harness.cycleNext();
        }
        case "compare" -> {
          harness.populateCompare();
          harness.cycleNext();
        }
        default -> harness.cycleNext();
      }
    }
  }

  private void capture(String name) {
    int bw = Gdx.graphics.getBackBufferWidth();
    int bh = Gdx.graphics.getBackBufferHeight();
    byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, bw, bh, true);
    Pixmap pm = new Pixmap(bw, bh, Format.RGBA8888);
    pm.getPixels().put(pixels);
    pm.getPixels().position(0);
    PixmapIO.writePNG(Gdx.files.absolute(out + "/" + name), pm);
    pm.dispose();
  }

  @Override
  public void resize(int width, int height) {
    harness.resize(width, height);
  }

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  @Override
  public void dispose() {
    harness.dispose();
  }
}
