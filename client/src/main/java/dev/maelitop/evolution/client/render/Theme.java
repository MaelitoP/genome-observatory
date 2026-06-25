package dev.maelitop.evolution.client.render;

import com.badlogic.gdx.graphics.Color;

/** Exact design tokens from the Genome Observatory handoff: palette and layout measurements. */
public final class Theme {

  private Theme() {}

  public static final Color FIELD = Color.valueOf("060A12");
  public static final Color APP_BG = Color.valueOf("0A0E15");
  public static final Color PANEL = Color.valueOf("0B1018");
  public static final Color PANEL_ALT = Color.valueOf("0E1420");
  public static final Color NETWORK_BG = Color.valueOf("070B12");
  public static final Color HAIRLINE = Color.valueOf("1B2433");
  public static final Color BORDER_MUTED = Color.valueOf("1F2A3B");

  public static final Color ACCENT = Color.valueOf("A78BFA");
  public static final Color ACCENT_BRIGHT = Color.valueOf("CDB6FF");
  public static final Color ACCENT_DEEP = Color.valueOf("7C5CF0");

  public static final Color HERBIVORE = Color.valueOf("2DD4BF");
  public static final Color CARNIVORE = Color.valueOf("FB7185");
  public static final Color CARNIVORE_SPIKE = Color.valueOf("FDA4AF");
  public static final Color FOOD = Color.valueOf("EEF2F7");

  public static final Color BEST = Color.valueOf("34D399");
  public static final Color MEAN = Color.valueOf("FBBF24");
  public static final Color DIVERSITY = Color.valueOf("38BDF8");

  public static final Color WEIGHT_POS = Color.valueOf("34D399");
  public static final Color WEIGHT_NEG = Color.valueOf("F87171");
  public static final Color NODE_INPUT = Color.valueOf("38BDF8");
  public static final Color NODE_BIAS = Color.valueOf("C4B5FD");
  public static final Color NODE_HIDDEN = Color.valueOf("64748B");
  public static final Color NODE_OUTPUT = Color.valueOf("A78BFA");
  public static final Color EDGE_DISABLED = Color.valueOf("3A465C");

  public static final Color TEXT_PRIMARY = Color.valueOf("E6ECF4");
  public static final Color TEXT_BODY = Color.valueOf("D6DEEA");
  public static final Color TEXT_BODY_DIM = Color.valueOf("CDD6E2");
  public static final Color TEXT_MUTED = Color.valueOf("8A97A8");
  public static final Color TEXT_FAINT = Color.valueOf("6B7888");
  public static final Color TEXT_FAINT_2 = Color.valueOf("5A6675");
  public static final Color TEXT_FAINT_3 = Color.valueOf("475569");

  public static final Color STATUS_RUNNING = Color.valueOf("34D399");
  public static final Color STATUS_CONNECTED = Color.valueOf("2DD4BF");
  public static final Color PAUSED = Color.valueOf("FBBF24");

  public static final Color KEYCAP_FILL = Color.valueOf("161E2C");
  public static final Color KEYCAP_BORDER = Color.valueOf("2A364C");

  /** Selection tint behind a chosen row/element (violet, low alpha over the panel). */
  public static final Color SELECT_TINT = new Color(0.655f, 0.545f, 0.980f, 0.12f);

  public static final Color GRID_DOT = new Color(120 / 255f, 160 / 255f, 210 / 255f, 0.10f);
  public static final Color GRID_LINE = new Color(140 / 255f, 160 / 255f, 200 / 255f, 0.05f);
  public static final Color CHART_GRID = new Color(140 / 255f, 160 / 255f, 200 / 255f, 0.08f);
  public static final Color WORLD_BOUNDS = new Color(167 / 255f, 139 / 255f, 250 / 255f, 0.18f);
  public static final Color PAUSE_VEIL = new Color(6 / 255f, 10 / 255f, 18 / 255f, 0.45f);

  public static final float HUD_HEIGHT = 48f;
  public static final float HINT_HEIGHT = 38f;
  public static final float PANEL_PAD = 14f;
  public static final float RADIUS = 6f;
  public static final float WINDOW_RADIUS = 8f;

  public static final float INSPECTOR_W = 316f;
  public static final float RUNLIST_W = 262f;
  public static final float CHAMPION_W = 300f;
  public static final float METRICS_W = 344f;

  public static final float WORLD_W = 960f;
  public static final float WORLD_H = 600f;
}
