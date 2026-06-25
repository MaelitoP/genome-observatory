package dev.maelitop.evolution.client;

import java.net.URI;
import java.util.List;

/** Bridge used by the offscreen capture tool to drive the real client against a running server. */
public final class CaptureHarness {

  private WorldClient world;
  private RunApiClient api;
  private EvolutionClient game;

  public void create() {
    world = new WorldClient(URI.create("ws://localhost:7070/world"));
    world.start();
    api = new RunApiClient(URI.create("http://localhost:7070"));
    game = new EvolutionClient(world, api);
    game.create();
  }

  public void render() {
    game.render();
  }

  public void resize(int width, int height) {
    game.resize(width, height);
  }

  public void cycleNext() {
    game.cycleNext();
  }

  public void openNewRun() {
    game.runs().form().toggle();
  }

  public void closeNewRun() {
    game.runs().form().close();
  }

  public void populateCompare() {
    RunBrowser browser = game.runs();
    List<RunSummary> runs = browser.runs();
    if (!runs.isEmpty()) {
      browser.select(0);
      browser.toggleCompareSelected();
    }
    if (runs.size() >= 2) {
      browser.select(1);
      browser.toggleCompareSelected();
    }
  }

  public void dispose() {
    game.dispose();
  }
}
