package dev.maelitop.evolution.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import dev.maelitop.evolution.protocol.Team;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

final class AnalyticsScreen implements Screen {

  private static final float ROW_HEIGHT = 22f;
  private static final float NODE_RADIUS = 6f;
  private static final Color[] PALETTE = {
    Color.CYAN, Color.LIME, Color.GOLD, Color.MAGENTA, Color.ORANGE, Color.SCARLET
  };

  private final RunApiClient api;
  private final EvolutionClient game;
  private final ShapeRenderer shapes = new ShapeRenderer();
  private final SpriteBatch batch = new SpriteBatch();
  private final BitmapFont font = new BitmapFont();
  private final StartRunForm form = new StartRunForm();
  private final Set<Long> compared = new LinkedHashSet<>();

  private int selected;
  private Team team = Team.HERBIVORE;
  private long loadedRun = -1;
  private Team loadedTeam;

  AnalyticsScreen(RunApiClient api, EvolutionClient game) {
    this.api = api;
    this.game = game;
  }

  @Override
  public void show() {
    Gdx.input.setInputProcessor(null);
    loadedRun = -1;
    var _ = api.refreshRuns();
  }

  @Override
  public void render(float delta) {
    if (Gdx.input.isKeyJustPressed(Keys.TAB)) {
      game.showWorld();
      return;
    }
    handleInput();
    syncSelection();

    float w = Gdx.graphics.getWidth();
    float h = Gdx.graphics.getHeight();
    float listX = 10f;
    float listW = 230f;
    float rightX = listX + listW + 20f;
    float rightW = w - rightX - 20f;
    float chartH = h * 0.40f;
    float chartY = h * 0.52f;
    float topoH = h * 0.44f;
    float topoY = 20f;

    ScreenUtils.clear(0.07f, 0.07f, 0.09f, 1);
    shapes.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
    batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);

    shapes.begin(ShapeRenderer.ShapeType.Line);
    border(listX, topoY, listW, h - topoY - 30f);
    border(rightX, chartY, rightW, chartH);
    border(rightX, topoY, rightW, topoH);
    drawChartLines(rightX, chartY, rightW, chartH);
    drawTopologyEdges(rightX, topoY, rightW, topoH);
    shapes.end();

    shapes.begin(ShapeRenderer.ShapeType.Filled);
    highlightSelected(listX, listW, h);
    drawTopologyNodes(rightX, topoY, rightW, topoH);
    shapes.end();

    batch.begin();
    drawText(listX, rightX, chartY, chartH, w, h);
    batch.end();
  }

  private void handleInput() {
    if (form.open()) {
      handleFormInput();
      return;
    }
    List<RunSummary> runs = api.runs();
    if (Gdx.input.isKeyJustPressed(Keys.UP)) {
      selected--;
    }
    if (Gdx.input.isKeyJustPressed(Keys.DOWN)) {
      selected++;
    }
    if (Gdx.input.isKeyJustPressed(Keys.T)) {
      team = team == Team.HERBIVORE ? Team.CARNIVORE : Team.HERBIVORE;
      loadedTeam = null;
    }
    if (Gdx.input.isKeyJustPressed(Keys.C) && !runs.isEmpty()) {
      long id = runs.get(clamp(selected, runs.size())).id();
      if (!compared.remove(id)) {
        compared.add(id);
        var _ = api.loadGenerations(id);
      }
    }
    if (Gdx.input.isKeyJustPressed(Keys.R)) {
      loadedRun = -1;
      var _ = api.refreshRuns();
    }
    if (Gdx.input.isKeyJustPressed(Keys.E)) {
      api.exportChampion(Path.of("champion-" + currentRunId() + "-" + team + ".json"));
    }
    if (Gdx.input.isKeyJustPressed(Keys.N)) {
      form.toggle();
    }
  }

  private void handleFormInput() {
    if (Gdx.input.isKeyJustPressed(Keys.UP)) {
      form.prevField();
    }
    if (Gdx.input.isKeyJustPressed(Keys.DOWN)) {
      form.nextField();
    }
    for (int key = Keys.NUM_0; key <= Keys.NUM_9; key++) {
      if (Gdx.input.isKeyJustPressed(key)) {
        form.typeDigit(key - Keys.NUM_0);
      }
    }
    if (Gdx.input.isKeyJustPressed(Keys.BACKSPACE)) {
      form.backspace();
    }
    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      api.startRun(form.seed(), form.generations(), form.carnivores());
      form.close();
    }
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      form.close();
    }
  }

  private void syncSelection() {
    List<RunSummary> runs = api.runs();
    if (runs.isEmpty()) {
      return;
    }
    selected = clamp(selected, runs.size());
    long id = runs.get(selected).id();
    if (id != loadedRun || team != loadedTeam) {
      loadedRun = id;
      loadedTeam = team;
      var _ = api.loadGenerations(id);
      var _ = api.loadChampion(id, team);
    }
  }

  private void border(float x, float y, float w, float h) {
    shapes.setColor(0.3f, 0.3f, 0.35f, 1);
    shapes.rect(x, y, w, h);
  }

  private void highlightSelected(float listX, float listW, float h) {
    List<RunSummary> runs = api.runs();
    if (runs.isEmpty()) {
      return;
    }
    float y = h - 60f - selected * ROW_HEIGHT;
    shapes.setColor(0.18f, 0.22f, 0.30f, 1);
    shapes.rect(listX + 2, y - 4, listW - 4, ROW_HEIGHT);
  }

  private void drawChartLines(float x, float y, float w, float h) {
    if (!compared.isEmpty()) {
      drawComparison(x, y, w, h);
      return;
    }
    List<GenerationRecordView.Stats> points = teamPoints(currentRunId());
    if (points.size() < 2) {
      return;
    }
    double[] best = column(points, GenerationRecordView.Stats::bestFitness);
    double[] mean = column(points, GenerationRecordView.Stats::meanFitness);
    double[] diversity = column(points, GenerationRecordView.Stats::diversity);
    double max = Math.max(maxOf(best), maxOf(mean));
    polyline(best, x, y, w, h, 0, max, Color.LIME);
    polyline(mean, x, y, w, h, 0, max, Color.GOLD);
    polyline(diversity, x, y, w, h, 0, maxOf(diversity), Color.SKY);
  }

  private void drawComparison(float x, float y, float w, float h) {
    double max = 0;
    for (long id : compared) {
      for (GenerationRecordView.Stats s : teamPoints(id)) {
        max = Math.max(max, s.bestFitness());
      }
    }
    if (max <= 0) {
      return;
    }
    int color = 0;
    for (long id : compared) {
      List<GenerationRecordView.Stats> points = teamPoints(id);
      if (points.size() >= 2) {
        double[] best = column(points, GenerationRecordView.Stats::bestFitness);
        polyline(best, x, y, w, h, 0, max, PALETTE[color % PALETTE.length]);
      }
      color++;
    }
  }

  private void polyline(
      double[] values, float x, float y, float w, float h, double min, double max, Color color) {
    shapes.setColor(color);
    double span = max - min;
    if (span <= 0) {
      span = 1;
    }
    float stepX = w / (values.length - 1);
    for (int i = 0; i < values.length - 1; i++) {
      float y0 = y + (float) ((values[i] - min) / span) * h;
      float y1 = y + (float) ((values[i + 1] - min) / span) * h;
      shapes.line(x + i * stepX, y0, x + (i + 1) * stepX, y1);
    }
  }

  private void drawTopologyEdges(float x, float y, float w, float h) {
    api.champion()
        .ifPresent(
            genome -> {
              Map<Integer, float[]> positions = layout(genome, x, y, w, h);
              for (GenomeView.ConnectionView c : genome.connections()) {
                if (!c.enabled()) {
                  continue;
                }
                float[] from = positions.get(c.in());
                float[] to = positions.get(c.out());
                if (from == null || to == null) {
                  continue;
                }
                float magnitude = (float) Math.min(1, Math.abs(c.weight()));
                if (c.weight() >= 0) {
                  shapes.setColor(0.2f, 0.9f, 0.4f, 0.2f + 0.8f * magnitude);
                } else {
                  shapes.setColor(0.9f, 0.3f, 0.3f, 0.2f + 0.8f * magnitude);
                }
                shapes.line(from[0], from[1], to[0], to[1]);
              }
            });
  }

  private void drawTopologyNodes(float x, float y, float w, float h) {
    api.champion()
        .ifPresent(
            genome -> {
              Map<Integer, float[]> positions = layout(genome, x, y, w, h);
              for (GenomeView.NodeView node : genome.nodes()) {
                float[] p = positions.get(node.id());
                if (p == null) {
                  continue;
                }
                shapes.setColor(nodeColor(node.type()));
                shapes.circle(p[0], p[1], NODE_RADIUS);
              }
            });
  }

  private static Color nodeColor(String type) {
    return switch (type) {
      case "INPUT" -> Color.SKY;
      case "BIAS" -> Color.GRAY;
      case "OUTPUT" -> Color.ORANGE;
      default -> Color.WHITE;
    };
  }

  private static Map<Integer, float[]> layout(
      GenomeView genome, float x, float y, float w, float h) {
    List<Integer> left = new ArrayList<>();
    List<Integer> middle = new ArrayList<>();
    List<Integer> right = new ArrayList<>();
    for (GenomeView.NodeView node : genome.nodes()) {
      switch (node.type()) {
        case "INPUT", "BIAS" -> left.add(node.id());
        case "OUTPUT" -> right.add(node.id());
        default -> middle.add(node.id());
      }
    }
    Map<Integer, float[]> positions = new HashMap<>();
    place(left, x + 30f, y, h, positions);
    place(middle, x + w / 2f, y, h, positions);
    place(right, x + w - 30f, y, h, positions);
    return positions;
  }

  private static void place(
      List<Integer> ids, float columnX, float y, float h, Map<Integer, float[]> out) {
    for (int i = 0; i < ids.size(); i++) {
      float ny = y + h - (i + 1) * (h / (ids.size() + 1));
      out.put(ids.get(i), new float[] {columnX, ny});
    }
  }

  private void drawText(float listX, float rightX, float chartY, float chartH, float w, float h) {
    font.setColor(Color.WHITE);
    font.draw(batch, "RUNS", listX + 6, h - 36f);
    List<RunSummary> runs = api.runs();
    if (runs.isEmpty()) {
      font.draw(batch, "no runs yet — press [N]", listX + 6, h - 60f);
    }
    for (int i = 0; i < runs.size(); i++) {
      RunSummary run = runs.get(i);
      String mark = compared.contains(run.id()) ? "* " : "  ";
      String label =
          mark + "#" + run.id() + "  g" + run.generations() + (run.carnivores() > 0 ? "  co" : "");
      font.setColor(i == selected ? Color.WHITE : Color.LIGHT_GRAY);
      font.draw(batch, label, listX + 6, h - 60f - i * ROW_HEIGHT + 14f);
    }

    font.setColor(Color.WHITE);
    String chartTitle =
        compared.isEmpty()
            ? "FITNESS — best (green) mean (gold) diversity (blue), team " + team
            : "COMPARE — best fitness, " + compared.size() + " runs, team " + team;
    font.draw(batch, chartTitle, rightX + 4, chartY + chartH + 18f);
    font.draw(batch, "NETWORK — champion topology, team " + team, rightX + 4, h * 0.50f);

    font.setColor(Color.LIGHT_GRAY);
    font.draw(
        batch,
        "[Up/Dn] select  [T] team  [C] compare  [N] new run  [E] export  [R] refresh  [TAB] world",
        listX,
        16f);

    if (form.open()) {
      drawForm(w, h);
    }
  }

  private void drawForm(float w, float h) {
    float fx = w / 2f - 160f;
    float fy = h / 2f + 40f;
    font.setColor(Color.WHITE);
    font.draw(batch, "NEW RUN  ([Up/Dn] field, digits, [ENTER] start, [ESC] cancel)", fx, fy + 30f);
    drawField("seed", StartRunForm.Field.SEED, fx, fy);
    drawField("generations", StartRunForm.Field.GENERATIONS, fx, fy - 24f);
    drawField("carnivores", StartRunForm.Field.CARNIVORES, fx, fy - 48f);
  }

  private void drawField(String label, StartRunForm.Field field, float x, float y) {
    boolean active = form.field() == field;
    font.setColor(active ? Color.GOLD : Color.LIGHT_GRAY);
    font.draw(batch, (active ? "> " : "  ") + label + ": " + form.text(field), x, y);
  }

  private long currentRunId() {
    List<RunSummary> runs = api.runs();
    return runs.isEmpty() ? -1 : runs.get(clamp(selected, runs.size())).id();
  }

  private List<GenerationRecordView.Stats> teamPoints(long runId) {
    List<GenerationRecordView.Stats> points = new ArrayList<>();
    for (GenerationRecordView record : api.generations(runId)) {
      if (record.team() == team) {
        points.add(record.stats());
      }
    }
    return points;
  }

  private static double[] column(
      List<GenerationRecordView.Stats> points, ToDoubleFunction<GenerationRecordView.Stats> field) {
    return points.stream().mapToDouble(field).toArray();
  }

  private static int clamp(int index, int size) {
    return Math.clamp(index, 0, size - 1);
  }

  private static double maxOf(double[] values) {
    double max = 0;
    for (double v : values) {
      max = Math.max(max, v);
    }
    return max;
  }

  @Override
  public void resize(int width, int height) {}

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
