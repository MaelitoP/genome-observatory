package dev.maelitop.evolution.client.view;

import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.Fonts;
import dev.maelitop.evolution.client.render.RenderContext;
import dev.maelitop.evolution.client.render.Theme;
import java.util.List;

/**
 * Bottom control-hint bar: a row of key-cap chips with muted descriptions; TAB hint at the right.
 */
public final class HintBar {

  public static final float HEIGHT = Theme.HINT_HEIGHT;

  public record Hint(String key, String desc) {}

  private HintBar() {}

  public static void draw(RenderContext rc, float width, List<Hint> hints, Hint tab) {
    Draw d = rc.draw;
    Fonts f = rc.fonts;
    float mid = HEIGHT / 2f;
    float capY = mid - Draw.KEYCAP_H / 2f;

    d.rect(0, 0, width, HEIGHT, Theme.PANEL);
    d.line(0, HEIGHT, width, HEIGHT, Theme.HAIRLINE);

    float x = 16f;
    for (Hint hint : hints) {
      float capW = d.keyCap(x, capY, hint.key());
      x += capW + 7f;
      d.textMid(f.mono11, hint.desc(), x, mid, Theme.TEXT_MUTED);
      x += f.width(f.mono11, hint.desc()) + 18f;
    }

    if (tab != null) {
      float descW = f.width(f.mono11, tab.desc());
      float capW = f.width(f.mono11, tab.key()) + 12f;
      float blockW = capW + 7f + descW;
      float bx = width - 16f - blockW;
      float used = d.keyCap(bx, capY, tab.key(), Theme.ACCENT_BRIGHT, Theme.ACCENT);
      d.textMid(f.mono11, tab.desc(), bx + used + 7f, mid, Theme.ACCENT);
    }
  }
}
