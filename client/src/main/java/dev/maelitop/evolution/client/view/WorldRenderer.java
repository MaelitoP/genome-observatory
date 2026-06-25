package dev.maelitop.evolution.client.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.RenderContext;
import dev.maelitop.evolution.client.render.Theme;
import dev.maelitop.evolution.protocol.CreatureSnapshot;
import dev.maelitop.evolution.protocol.EntitySnapshot;
import dev.maelitop.evolution.protocol.FoodSnapshot;
import dev.maelitop.evolution.protocol.Team;
import dev.maelitop.evolution.protocol.WorldSnapshot;

/** Draws the top-down arena: dot grid, world bounds, food and creatures (server colors only). */
public final class WorldRenderer {

  private static final float HERB_R = 7f;
  private static final float CARN_R = 8f;
  private static final float FOOD_R = 2.2f;

  private final Color body = new Color();
  private final Color ring = new Color();

  /** View transform: world (wx,wy) maps to screen (ox + wx*scale, oy + wy*scale). */
  public record View(float ox, float oy, float scale) {}

  public void draw(
      RenderContext rc,
      WorldSnapshot snap,
      float worldW,
      float worldH,
      View v,
      long selectedId,
      float ax,
      float ay,
      float aw,
      float ah) {
    Draw d = rc.draw;
    d.rect(ax, ay, aw, ah, Theme.FIELD);
    drawGrid(d, v, worldW, worldH);
    drawBounds(d, v, worldW, worldH);

    if (snap == null) {
      return;
    }

    for (EntitySnapshot e : snap.entities()) {
      if (e instanceof FoodSnapshot food) {
        d.glowDisc(sx(v, food.x()), sy(v, food.y()), FOOD_R, Theme.FOOD, 0.4f);
      } else if (e instanceof CreatureSnapshot c) {
        d.glowDisc(sx(v, c.x()), sy(v, c.y()), radius(c), serverColor(body, c), 0.5f);
      }
    }

    for (EntitySnapshot e : snap.entities()) {
      if (e instanceof FoodSnapshot food) {
        d.circle(sx(v, food.x()), sy(v, food.y()), FOOD_R, Theme.FOOD, 0.75f);
      }
    }

    for (EntitySnapshot e : snap.entities()) {
      if (e instanceof CreatureSnapshot c) {
        drawCreature(rc, c, v, c.id() == selectedId);
      }
    }
  }

  private void drawGrid(Draw d, View v, float worldW, float worldH) {
    for (float x = 0; x <= worldW; x += 26f) {
      for (float y = 0; y <= worldH; y += 26f) {
        d.circle(sx(v, x), sy(v, y), 1f, Theme.GRID_DOT);
      }
    }
  }

  private void drawBounds(Draw d, View v, float worldW, float worldH) {
    float x0 = v.ox;
    float y0 = v.oy;
    float x1 = v.ox + worldW * v.scale;
    float y1 = v.oy + worldH * v.scale;
    d.line(x0, y0, x1, y0, Theme.WORLD_BOUNDS);
    d.line(x1, y0, x1, y1, Theme.WORLD_BOUNDS);
    d.line(x1, y1, x0, y1, Theme.WORLD_BOUNDS);
    d.line(x0, y1, x0, y0, Theme.WORLD_BOUNDS);
  }

  private void drawCreature(RenderContext rc, CreatureSnapshot c, View v, boolean selected) {
    Draw d = rc.draw;
    float cx = sx(v, c.x());
    float cy = sy(v, c.y());
    float r = radius(c);
    serverColor(body, c);
    ring.set(body).lerp(Color.WHITE, 0.35f);
    float heading = (float) Math.toRadians(c.angle());

    d.circle(cx, cy, r, body);
    d.strokeCircle(cx, cy, r + 2.5f, 1.4f, ring, 0.95f);

    float energy = MathUtils.clamp((float) c.energy(), 0f, 1f);
    if (energy > 0) {
      d.arc(cx, cy, r + 5f, 90f, -energy * 360f, 2f, ring, 0.95f);
    }

    if (c.team() == Team.CARNIVORE) {
      float len = r + MathUtils.clamp((float) c.spike(), 0f, 1f) * 22f;
      float ex = cx + MathUtils.cos(heading) * len;
      float ey = cy + MathUtils.sin(heading) * len;
      d.glowLine(cx, cy, ex, ey, 2.2f, ring, 0.4f);
      d.thickLine(cx, cy, ex, ey, 2f, ring, 1f);
    } else {
      float len = r + 5f;
      d.line(
          cx, cy, cx + MathUtils.cos(heading) * len, cy + MathUtils.sin(heading) * len, ring, 0.9f);
    }

    if (selected) {
      drawSelection(rc, cx, cy, r);
    }
  }

  private void drawSelection(RenderContext rc, float cx, float cy, float r) {
    Draw d = rc.draw;
    float sel = r + 12f;
    d.dashedCircle(cx, cy, sel, 12f, 12f, rc.time() * 60f, Theme.ACCENT);
    d.line(cx - 22f, cy, cx - 14f, cy, Theme.ACCENT, 0.5f);
    d.line(cx + 14f, cy, cx + 22f, cy, Theme.ACCENT, 0.5f);
    d.line(cx, cy - 22f, cx, cy - 14f, Theme.ACCENT, 0.5f);
    d.line(cx, cy + 14f, cx, cy + 22f, Theme.ACCENT, 0.5f);
  }

  private static float radius(CreatureSnapshot c) {
    return c.team() == Team.CARNIVORE ? CARN_R : HERB_R;
  }

  private static Color serverColor(Color out, CreatureSnapshot c) {
    return out.set(c.color().r(), c.color().g(), c.color().b(), 1f);
  }

  private static float sx(View v, double wx) {
    return v.ox + (float) wx * v.scale;
  }

  private static float sy(View v, double wy) {
    return v.oy + (float) wy * v.scale;
  }
}
