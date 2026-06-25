package dev.maelitop.evolution.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.MathUtils;
import dev.maelitop.evolution.client.render.Theme;
import dev.maelitop.evolution.client.view.AgentInspector;
import dev.maelitop.evolution.client.view.HintBar;
import dev.maelitop.evolution.client.view.HudBar;
import dev.maelitop.evolution.client.view.WorldRenderer;
import dev.maelitop.evolution.protocol.CreatureSnapshot;
import dev.maelitop.evolution.protocol.EntitySnapshot;
import dev.maelitop.evolution.protocol.HelloMessage;
import dev.maelitop.evolution.protocol.WorldSnapshot;
import java.util.List;

final class WorldScreen extends ObservatoryScreen {

  private final WorldClient world;
  private final ControlInput control = new ControlInput();
  private final WorldRenderer worldRenderer = new WorldRenderer();
  private final AgentInspector inspector = new AgentInspector();

  private float panX;
  private float panY;
  private float zoom = 1f;
  private long selectedId = -1;

  WorldScreen(EvolutionClient app, WorldClient world) {
    super(app);
    this.world = world;
  }

  @Override
  public void show() {
    super.show();
    Gdx.input.setInputProcessor(
        new InputAdapter() {
          @Override
          public boolean scrolled(float amountX, float amountY) {
            zoom = MathUtils.clamp(zoom * (1f - amountY * 0.1f), 0.4f, 4f);
            return true;
          }
        });
  }

  @Override
  protected void input(float delta) {
    if (Gdx.input.isKeyJustPressed(Keys.P)) {
      control.on(ControlInput.Command.TOGGLE_PAUSE).ifPresent(world::send);
    }
    if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
      control.on(ControlInput.Command.STEP).ifPresent(world::send);
    }
    if (Gdx.input.isKeyJustPressed(Keys.RIGHT_BRACKET)) {
      control.on(ControlInput.Command.FASTER).ifPresent(world::send);
    }
    if (Gdx.input.isKeyJustPressed(Keys.LEFT_BRACKET)) {
      control.on(ControlInput.Command.SLOWER).ifPresent(world::send);
    }
    float step = 320f * delta / Math.max(zoom, 0.4f);
    if (Gdx.input.isKeyPressed(Keys.LEFT)) {
      panX += step;
    }
    if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
      panX -= step;
    }
    if (Gdx.input.isKeyPressed(Keys.UP)) {
      panY -= step;
    }
    if (Gdx.input.isKeyPressed(Keys.DOWN)) {
      panY += step;
    }
  }

  @Override
  protected void drawBody(float delta) {
    float arenaW = bodyW - Theme.INSPECTOR_W;
    float arenaX = bodyX;
    WorldSnapshot snap = world.snapshot();
    WorldRenderer.View view = computeView(arenaX, bodyY, arenaW, bodyH);

    if (Gdx.input.justTouched()) {
      pick(snap, view, arenaX, arenaW);
    }

    rc.draw.clip(arenaX, bodyY, arenaW, bodyH, rc.density());
    worldRenderer.draw(
        rc, snap, worldW(), worldH(), view, selectedId, arenaX, bodyY, arenaW, bodyH);
    rc.draw.clipEnd();

    if (snap == null) {
      rc.draw.textCentered(
          rc.fonts.mono12,
          "connecting to world …",
          arenaX + arenaW / 2f,
          bodyY + bodyH / 2f,
          Theme.TEXT_FAINT);
    } else if (control.paused()) {
      rc.draw.rect(arenaX, bodyY, arenaW, bodyH, Theme.PAUSE_VEIL, Theme.PAUSE_VEIL.a);
      rc.draw.textCentered(
          rc.fonts.mono14,
          "‖ PAUSED — SPACE to step",
          arenaX + arenaW / 2f,
          bodyY + bodyH - 40f,
          Theme.PAUSED);
    }

    inspector.draw(rc, bodyX + arenaW, bodyY, Theme.INSPECTOR_W, bodyH, findSelected(snap));
  }

  @Override
  protected void drawChrome() {
    WorldSnapshot snap = world.snapshot();
    String speedLabel = control.paused() ? "0×" : trim(control.speed()) + "×";
    List<HudBar.Readout> readouts =
        List.of(
            new HudBar.Readout(
                "TICK", snap == null ? "—" : String.valueOf(snap.tick()), Theme.TEXT_PRIMARY),
            new HudBar.Readout(
                "GEN", snap == null ? "—" : String.valueOf(snap.generation()), Theme.TEXT_PRIMARY),
            new HudBar.Readout(
                "POP", snap == null ? "—" : String.valueOf(snap.population()), Theme.TEXT_PRIMARY),
            new HudBar.Readout("SPEED", speedLabel, Theme.ACCENT_BRIGHT));
    HudBar.draw(rc, width, height, "world", readouts, status(snap));

    HintBar.draw(
        rc,
        width,
        List.of(
            new HintBar.Hint("P", "pause"),
            new HintBar.Hint("SPACE", "step"),
            new HintBar.Hint("[ ]", "speed"),
            new HintBar.Hint("←↑↓→", "pan")),
        new HintBar.Hint("TAB", "analytics"));
  }

  private HudBar.Status status(WorldSnapshot snap) {
    if (snap == null) {
      return new HudBar.Status("connecting", Theme.PAUSED);
    }
    if (control.paused()) {
      return new HudBar.Status("paused", Theme.PAUSED);
    }
    return new HudBar.Status("connected", Theme.STATUS_CONNECTED);
  }

  private WorldRenderer.View computeView(float ax, float ay, float aw, float ah) {
    float fit = Math.min(aw / worldW(), ah / worldH());
    float scale = fit * zoom;
    float ox = ax + (aw - worldW() * scale) / 2f + panX;
    float oy = ay + (ah - worldH() * scale) / 2f + panY;
    return new WorldRenderer.View(ox, oy, scale);
  }

  private void pick(WorldSnapshot snap, WorldRenderer.View view, float arenaX, float arenaW) {
    if (snap == null) {
      return;
    }
    float mx = Gdx.input.getX();
    float my = height - Gdx.input.getY();
    if (mx < arenaX || mx > arenaX + arenaW || my < bodyY || my > bodyY + bodyH) {
      return;
    }
    long best = -1;
    float bestD = 16f * 16f;
    for (EntitySnapshot e : snap.entities()) {
      if (e instanceof CreatureSnapshot c) {
        float dx = view.ox() + (float) c.x() * view.scale() - mx;
        float dy = view.oy() + (float) c.y() * view.scale() - my;
        float d2 = dx * dx + dy * dy;
        if (d2 < bestD) {
          bestD = d2;
          best = c.id();
        }
      }
    }
    if (best >= 0) {
      selectedId = best;
    }
  }

  private CreatureSnapshot findSelected(WorldSnapshot snap) {
    if (snap == null || selectedId < 0) {
      return null;
    }
    for (EntitySnapshot e : snap.entities()) {
      if (e instanceof CreatureSnapshot c && c.id() == selectedId) {
        return c;
      }
    }
    return null;
  }

  private float worldW() {
    HelloMessage hello = world.hello();
    return hello != null ? hello.worldConfig().width() : Theme.WORLD_W;
  }

  private float worldH() {
    HelloMessage hello = world.hello();
    return hello != null ? hello.worldConfig().height() : Theme.WORLD_H;
  }

  private static String trim(double v) {
    if (v == Math.rint(v)) {
      return String.valueOf((long) v);
    }
    return String.valueOf(v);
  }
}
