package dev.maelitop.evolution.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import dev.maelitop.evolution.client.render.Theme;
import java.net.URI;

public final class ClientMain {

  private ClientMain() {}

  public static void main(String[] args) {
    WorldClient world = new WorldClient(URI.create("ws://localhost:7070/world"));
    world.start();
    RunApiClient api = new RunApiClient(URI.create("http://localhost:7070"));

    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("Genome Observatory");
    config.setWindowedMode(1280, 720);
    config.setResizable(true);
    config.setWindowSizeLimits(1100, 640, -1, -1);
    config.useVsync(true);
    config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
    config.setInitialBackgroundColor(Theme.APP_BG);
    new Lwjgl3Application(new EvolutionClient(world, api), config);
  }
}
