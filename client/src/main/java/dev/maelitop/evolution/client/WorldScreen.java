package dev.maelitop.evolution.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import dev.maelitop.evolution.protocol.Color;
import dev.maelitop.evolution.protocol.CreatureSnapshot;
import dev.maelitop.evolution.protocol.EntitySnapshot;
import dev.maelitop.evolution.protocol.FoodSnapshot;
import dev.maelitop.evolution.protocol.HelloMessage;
import dev.maelitop.evolution.protocol.WorldSnapshot;

final class WorldScreen implements Screen {

  private static final float PAN_SPEED = 600f;
  private static final float CREATURE_RADIUS = 14f;
  private static final float HEADING_LENGTH = 20f;
  private static final float FOOD_RADIUS = 4f;

  private final WorldClient client;
  private final EvolutionClient game;
  private final ControlInput control = new ControlInput();
  private final OrthographicCamera camera = new OrthographicCamera();
  private final ShapeRenderer shapes = new ShapeRenderer();
  private final SpriteBatch batch = new SpriteBatch();
  private final BitmapFont font = new BitmapFont();
  private boolean centered;

  WorldScreen(WorldClient client, EvolutionClient game) {
    this.client = client;
    this.game = game;
    camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
  }

  @Override
  public void show() {
    camera.viewportWidth = Gdx.graphics.getWidth();
    camera.viewportHeight = Gdx.graphics.getHeight();
    camera.update();
    Gdx.input.setInputProcessor(
        new InputAdapter() {
          @Override
          public boolean scrolled(float amountX, float amountY) {
            camera.zoom = MathUtils.clamp(camera.zoom + amountY * 0.1f, 0.1f, 10f);
            return true;
          }
        });
  }

  @Override
  public void render(float delta) {
    if (Gdx.input.isKeyJustPressed(Keys.TAB)) {
      game.showAnalytics();
      return;
    }
    centerOnWorld();
    handlePan();
    handleControl();
    camera.update();

    ScreenUtils.clear(0, 0, 0, 1);
    WorldSnapshot snap = client.snapshot();
    if (snap != null) {
      drawWorld(snap);
    }
    drawHud(snap);
  }

  private void centerOnWorld() {
    if (centered) {
      return;
    }
    HelloMessage hello = client.hello();
    if (hello != null) {
      camera.position.set(hello.worldConfig().width() / 2f, hello.worldConfig().height() / 2f, 0);
      centered = true;
    }
  }

  private void handlePan() {
    float step = PAN_SPEED * camera.zoom * Gdx.graphics.getDeltaTime();
    if (Gdx.input.isKeyPressed(Keys.LEFT)) {
      camera.position.x -= step;
    }
    if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
      camera.position.x += step;
    }
    if (Gdx.input.isKeyPressed(Keys.UP)) {
      camera.position.y += step;
    }
    if (Gdx.input.isKeyPressed(Keys.DOWN)) {
      camera.position.y -= step;
    }
  }

  private void handleControl() {
    if (Gdx.input.isKeyJustPressed(Keys.P)) {
      control.on(ControlInput.Command.TOGGLE_PAUSE).ifPresent(client::send);
    }
    if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
      control.on(ControlInput.Command.STEP).ifPresent(client::send);
    }
    if (Gdx.input.isKeyJustPressed(Keys.RIGHT_BRACKET)) {
      control.on(ControlInput.Command.FASTER).ifPresent(client::send);
    }
    if (Gdx.input.isKeyJustPressed(Keys.LEFT_BRACKET)) {
      control.on(ControlInput.Command.SLOWER).ifPresent(client::send);
    }
  }

  private void drawWorld(WorldSnapshot snap) {
    shapes.setProjectionMatrix(camera.combined);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    for (EntitySnapshot entity : snap.entities()) {
      switch (entity) {
        case FoodSnapshot food -> {
          shapes.setColor(1, 1, 1, 1);
          shapes.circle((float) food.x(), (float) food.y(), FOOD_RADIUS);
        }
        case CreatureSnapshot creature -> drawCreatureBody(creature);
      }
    }
    shapes.end();

    shapes.begin(ShapeRenderer.ShapeType.Line);
    for (EntitySnapshot entity : snap.entities()) {
      switch (entity) {
        case CreatureSnapshot creature -> drawCreatureHeading(creature);
        case FoodSnapshot ignored -> {}
      }
    }
    shapes.end();
  }

  private void drawCreatureBody(CreatureSnapshot creature) {
    if (creature.spike() > 0) {
      shapes.setColor(1, 1, 0, 1);
      shapes.circle((float) creature.x(), (float) creature.y(), (float) (creature.spike() * 40));
    }
    Color color = creature.color();
    shapes.setColor(color.r(), color.g(), color.b(), 1);
    shapes.circle((float) creature.x(), (float) creature.y(), CREATURE_RADIUS);
  }

  private void drawCreatureHeading(CreatureSnapshot creature) {
    shapes.setColor(1, 0, 0, 1);
    float rad = (float) Math.toRadians(creature.angle());
    shapes.line(
        (float) creature.x(),
        (float) creature.y(),
        (float) (creature.x() + Math.cos(rad) * HEADING_LENGTH),
        (float) (creature.y() + Math.sin(rad) * HEADING_LENGTH));
  }

  private void drawHud(WorldSnapshot snap) {
    batch
        .getProjectionMatrix()
        .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    batch.begin();
    font.setColor(1, 1, 1, 1);
    String line =
        snap == null
            ? "Connecting to ws://localhost:7070/world ..."
            : "tick="
                + snap.tick()
                + "  gen="
                + snap.generation()
                + "  pop="
                + snap.population()
                + (control.paused() ? "  [PAUSED]" : "");
    font.draw(batch, line, 10, Gdx.graphics.getHeight() - 10f);
    font.draw(batch, "[P] pause  [SPACE] step  [ [ / ] ] speed  [TAB] analytics", 10, 20f);
    batch.end();
  }

  @Override
  public void resize(int width, int height) {
    camera.viewportWidth = width;
    camera.viewportHeight = height;
    camera.update();
  }

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  @Override
  public void hide() {}

  @Override
  public void dispose() {
    shapes.dispose();
    batch.dispose();
    font.dispose();
  }
}
