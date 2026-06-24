package dev.maelitop.evolution.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import java.net.URI;

public final class ClientMain {

  private ClientMain() {}

  public static void main(String[] args) {
    WorldClient world = new WorldClient(URI.create("ws://localhost:7070/world"));
    world.connect();
    RunApiClient api = new RunApiClient(URI.create("http://localhost:7070"));

    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("genetic-evolution");
    config.setWindowedMode(1280, 720);
    config.setForegroundFPS(60);
    new Lwjgl3Application(new EvolutionClient(world, api), config);
  }
}
