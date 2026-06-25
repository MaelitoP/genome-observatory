package dev.maelitop.evolution.client;

import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.RenderContext;
import dev.maelitop.evolution.client.render.Theme;

/** Launch-form overlay: keyboard-driven Seed / Generations / Carnivores (the fields POST /runs). */
final class NewRunModal {

  private NewRunModal() {}

  static void draw(RenderContext rc, float width, float height, StartRunForm form) {
    Draw d = rc.draw;
    d.rect(0, 0, width, height, Theme.FIELD, 0.62f);

    float bw = 440f;
    float bh = 264f;
    float bx = (width - bw) / 2f;
    float by = (height - bh) / 2f;
    d.roundedRect(bx, by, bw, bh, Theme.WINDOW_RADIUS, Theme.PANEL_ALT);
    d.strokeRoundedRect(bx, by, bw, bh, Theme.WINDOW_RADIUS, Theme.HAIRLINE);

    float pad = 22f;
    float left = bx + pad;
    float right = bx + bw - pad;
    float top = by + bh - pad;

    d.text(rc.fonts.sansTitle, "New run", left, top, Theme.TEXT_PRIMARY);
    d.text(
        rc.fonts.mono11, "configure the next co-evolution run", left, top - 22f, Theme.TEXT_MUTED);

    float fy = top - 56f;
    fy =
        field(
            rc,
            left,
            right,
            fy,
            "Seed",
            form.text(StartRunForm.Field.SEED),
            form.field() == StartRunForm.Field.SEED);
    fy =
        field(
            rc,
            left,
            right,
            fy,
            "Generations",
            form.text(StartRunForm.Field.GENERATIONS),
            form.field() == StartRunForm.Field.GENERATIONS);
    field(
        rc,
        left,
        right,
        fy,
        "Carnivores (0 = herbivore-only)",
        form.text(StartRunForm.Field.CARNIVORES),
        form.field() == StartRunForm.Field.CARNIVORES);

    d.text(
        rc.fonts.mono11,
        "generations 1–" + StartRunForm.MAX_GENERATIONS,
        left,
        by + pad,
        Theme.TEXT_FAINT_3);
    float hx = right;
    hx -= rc.fonts.width(rc.fonts.mono11, "cancel");
    d.textMid(rc.fonts.mono11, "cancel", hx, by + pad + 5f, Theme.TEXT_MUTED);
    hx -= 6f;
    hx -= rc.fonts.width(rc.fonts.mono11, "ESC") + 12f;
    d.keyCap(hx, by + pad - 4f, "ESC");
    hx -= 16f;
    hx -= rc.fonts.width(rc.fonts.mono11, "start");
    d.textMid(rc.fonts.mono11, "start", hx, by + pad + 5f, Theme.ACCENT);
    hx -= 6f;
    hx -= rc.fonts.width(rc.fonts.mono11, "ENTER") + 12f;
    d.keyCap(hx, by + pad - 4f, "ENTER", Theme.ACCENT_BRIGHT, Theme.ACCENT);
  }

  private static float field(
      RenderContext rc,
      float left,
      float right,
      float y,
      String label,
      String value,
      boolean focused) {
    Draw d = rc.draw;
    d.textMid(rc.fonts.mono11, label, left, y, focused ? Theme.ACCENT_BRIGHT : Theme.TEXT_MUTED);
    float boxY = y - 28f;
    float boxH = 24f;
    float boxW = right - left;
    d.roundedRect(left, boxY, boxW, boxH, 5f, Theme.PANEL);
    if (focused) {
      d.strokeRoundedRect(left - 1.5f, boxY - 1.5f, boxW + 3f, boxH + 3f, 6f, Theme.ACCENT, 0.35f);
      d.strokeRoundedRect(left, boxY, boxW, boxH, 5f, Theme.ACCENT);
    } else {
      d.strokeRoundedRect(left, boxY, boxW, boxH, 5f, Theme.BORDER_MUTED);
    }
    String shown = value;
    boolean caretOn = focused && (rc.time() * 2f) % 1f < 0.55f;
    if (caretOn) {
      shown = value + "|";
    }
    d.textMid(
        rc.fonts.mono13,
        shown.isEmpty() ? " " : shown,
        left + 10f,
        boxY + boxH / 2f,
        Theme.TEXT_PRIMARY);
    return boxY - 18f;
  }
}
