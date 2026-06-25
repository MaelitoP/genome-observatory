package dev.maelitop.evolution.client;

import com.badlogic.gdx.Game;
import dev.maelitop.evolution.client.render.RenderContext;
import java.util.ArrayList;
import java.util.List;

/** libGDX game: owns the shared render context and the ordered screens that TAB cycles through. */
final class EvolutionClient extends Game {

  private final WorldClient world;
  private final RunApiClient api;
  private final List<ObservatoryScreen> screens = new ArrayList<>();

  private RenderContext renderContext;
  private RunBrowser runs;
  private int current;

  EvolutionClient(WorldClient world, RunApiClient api) {
    this.world = world;
    this.api = api;
  }

  @Override
  public void create() {
    renderContext = new RenderContext();
    runs = new RunBrowser(api);
    screens.add(new WorldScreen(this, world));
    screens.add(new AnalyticsScreen(this, runs));
    screens.add(new NetworkScreen(this, runs));
    screens.add(new CompareScreen(this, runs));
    setScreen(screens.get(0));
  }

  RenderContext context() {
    return renderContext;
  }

  RunBrowser runs() {
    return runs;
  }

  void cycleNext() {
    current = (current + 1) % screens.size();
    setScreen(screens.get(current));
  }

  void cyclePrev() {
    current = (current - 1 + screens.size()) % screens.size();
    setScreen(screens.get(current));
  }

  void goTo(int index) {
    current = Math.floorMod(index, screens.size());
    setScreen(screens.get(current));
  }

  @Override
  public void dispose() {
    for (ObservatoryScreen screen : screens) {
      screen.dispose();
    }
    if (renderContext != null) {
      renderContext.dispose();
    }
    world.close();
    api.close();
  }
}
