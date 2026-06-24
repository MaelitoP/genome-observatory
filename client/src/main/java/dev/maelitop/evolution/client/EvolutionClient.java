package dev.maelitop.evolution.client;

import com.badlogic.gdx.Game;

final class EvolutionClient extends Game {

  private final WorldClient world;
  private final RunApiClient api;
  private WorldScreen worldScreen;
  private AnalyticsScreen analyticsScreen;

  EvolutionClient(WorldClient world, RunApiClient api) {
    this.world = world;
    this.api = api;
  }

  @Override
  public void create() {
    worldScreen = new WorldScreen(world, this);
    analyticsScreen = new AnalyticsScreen(api, this);
    setScreen(worldScreen);
  }

  void showWorld() {
    setScreen(worldScreen);
  }

  void showAnalytics() {
    setScreen(analyticsScreen);
  }

  @Override
  public void dispose() {
    worldScreen.dispose();
    analyticsScreen.dispose();
    world.close();
    api.close();
  }
}
