package dev.maelitop.evolution.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import java.net.URI;

public final class ClientMain {

  private ClientMain() {}

  public static void main(String[] args) {
    WorldClient client = new WorldClient(URI.create("ws://localhost:7070/world"));
    client.connect();

    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("genetic-evolution");
    config.setWindowedMode(1280, 720);
    config.setForegroundFPS(60);
    new Lwjgl3Application(new EvolutionClient(client), config);
  }
}
