package dev.maelitop.evolution.client.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import dev.maelitop.evolution.client.GenomeView;
import dev.maelitop.evolution.client.GenomeView.ConnectionView;
import dev.maelitop.evolution.client.GenomeView.NodeView;
import dev.maelitop.evolution.client.render.Draw;
import dev.maelitop.evolution.client.render.RenderContext;
import dev.maelitop.evolution.client.render.Theme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Draws a genome as a layered left→right graph. Used full-screen, compact, and as an inset. */
public final class NetworkRenderer {

  private final Map<Integer, float[]> pos = new HashMap<>();
  private final Color scratch = new Color();

  /** Lays out node positions for the genome within the rect; call once per frame before draw. */
  public void layout(GenomeView g, float x, float y, float w, float h, boolean labeled) {
    pos.clear();
    List<NodeView> inputs = new ArrayList<>();
    List<NodeView> hidden = new ArrayList<>();
    List<NodeView> outputs = new ArrayList<>();
    for (NodeView n : g.nodes()) {
      switch (n.type()) {
        case "INPUT", "BIAS" -> inputs.add(n);
        case "OUTPUT" -> outputs.add(n);
        default -> hidden.add(n);
      }
    }
    float leftX = labeled ? x + 100f : x + w * 0.10f;
    float rightX = labeled ? x + w - 100f : x + w * 0.90f;
    float vtop = y + h - (labeled ? 46f : 12f);
    float vbot = y + (labeled ? 16f : 12f);

    place(inputs, leftX, vtop, vbot);
    place(outputs, rightX, vtop, vbot);
    if (hidden.size() > 5) {
      int half = (hidden.size() + 1) / 2;
      place(hidden.subList(0, half), leftX + (rightX - leftX) * 0.36f, vtop, vbot);
      place(hidden.subList(half, hidden.size()), leftX + (rightX - leftX) * 0.64f, vtop, vbot);
    } else {
      place(hidden, (leftX + rightX) / 2f, vtop, vbot);
    }
  }

  private void place(List<NodeView> nodes, float colX, float vtop, float vbot) {
    int n = nodes.size();
    for (int i = 0; i < n; i++) {
      float frac = (i + 0.5f) / n;
      float ny = vtop - frac * (vtop - vbot);
      pos.put(nodes.get(i).id(), new float[] {colX, ny});
    }
  }

  public void draw(
      RenderContext rc, GenomeView g, float nodeR, boolean labeled, boolean pulses, int hoverId) {
    Draw d = rc.draw;
    float time = rc.time();

    drawEdgeGlow(d, g);
    drawEdges(d, g);
    drawNodeHalos(d, g, nodeR);
    drawNodeFills(d, g, nodeR);
    drawNodeGlow(d, g, nodeR);
    if (pulses) {
      drawPulses(d, g, time);
    }
    if (labeled) {
      drawLabels(rc, g);
    } else if (hoverId >= 0) {
      float[] p = pos.get(hoverId);
      if (p != null) {
        d.text(rc.fonts.mono10, nodeTag(g, hoverId), p[0] + nodeR + 3, p[1] + 4, Theme.TEXT_BODY);
      }
    }
  }

  private void drawEdgeGlow(Draw d, GenomeView g) {
    for (ConnectionView c : g.connections()) {
      if (!c.enabled()) {
        continue;
      }
      float mag = (float) Math.min(1, Math.abs(c.weight()));
      if (mag < 0.45f) {
        continue;
      }
      float[] a = pos.get(c.in());
      float[] b = pos.get(c.out());
      if (a == null || b == null) {
        continue;
      }
      Color col = c.weight() >= 0 ? Theme.WEIGHT_POS : Theme.WEIGHT_NEG;
      d.glowLine(a[0], a[1], b[0], b[1], 1.2f + mag * 3.2f, col, 0.10f + mag * 0.12f);
    }
  }

  private void drawEdges(Draw d, GenomeView g) {
    for (ConnectionView c : g.connections()) {
      float[] a = pos.get(c.in());
      float[] b = pos.get(c.out());
      if (a == null || b == null) {
        continue;
      }
      if (!c.enabled()) {
        d.dashedLine(a[0], a[1], b[0], b[1], 4f, 4f, 0f, Theme.EDGE_DISABLED);
        continue;
      }
      float mag = (float) Math.min(1, Math.abs(c.weight()));
      Color col = c.weight() >= 0 ? Theme.WEIGHT_POS : Theme.WEIGHT_NEG;
      d.thickLine(a[0], a[1], b[0], b[1], 0.6f + mag * 2.4f, col, 0.16f + mag * 0.6f);
    }
  }

  private void drawNodeHalos(Draw d, GenomeView g, float r) {
    for (NodeView n : g.nodes()) {
      float[] p = pos.get(n.id());
      if (p != null) {
        d.circle(p[0], p[1], r + 2.2f, Theme.NETWORK_BG);
      }
    }
  }

  private void drawNodeFills(Draw d, GenomeView g, float r) {
    for (NodeView n : g.nodes()) {
      float[] p = pos.get(n.id());
      if (p != null) {
        d.circle(p[0], p[1], r, nodeColor(n.type()));
      }
    }
  }

  private void drawNodeGlow(Draw d, GenomeView g, float r) {
    for (NodeView n : g.nodes()) {
      if (n.type().equals("HIDDEN")) {
        continue;
      }
      float[] p = pos.get(n.id());
      if (p != null) {
        d.glowDisc(p[0], p[1], r, nodeColor(n.type()), 0.9f);
      }
    }
  }

  private void drawPulses(Draw d, GenomeView g, float time) {
    for (ConnectionView c : g.connections()) {
      if (!c.enabled()) {
        continue;
      }
      float mag = (float) Math.min(1, Math.abs(c.weight()));
      if (mag < 0.2f) {
        continue;
      }
      float[] a = pos.get(c.in());
      float[] b = pos.get(c.out());
      if (a == null || b == null) {
        continue;
      }
      float offset = ((c.in() * 31 + c.out()) % 100) / 100f;
      float t = (time * 0.45f + offset) % 1f;
      float px = MathUtils.lerp(a[0], b[0], t);
      float py = MathUtils.lerp(a[1], b[1], t);
      Color col = c.weight() >= 0 ? Theme.WEIGHT_POS : Theme.WEIGHT_NEG;
      d.glowDisc(px, py, 1.6f, col, 0.9f);
      d.circle(px, py, 1.4f, scratch.set(col.r, col.g, col.b, 0.9f));
    }
  }

  private void drawLabels(RenderContext rc, GenomeView g) {
    Draw d = rc.draw;
    int inputIdx = 0;
    int outputIdx = 0;
    for (NodeView n : g.nodes()) {
      float[] p = pos.get(n.id());
      if (p == null) {
        continue;
      }
      switch (n.type()) {
        case "INPUT" ->
            d.textMidRight(rc.fonts.mono11, "in " + inputIdx++, p[0] - 14f, p[1], Theme.TEXT_MUTED);
        case "BIAS" -> d.textMidRight(rc.fonts.mono11, "bias", p[0] - 14f, p[1], Theme.NODE_BIAS);
        case "OUTPUT" ->
            d.textMid(rc.fonts.mono11, "out " + outputIdx++, p[0] + 14f, p[1], Theme.ACCENT_BRIGHT);
        default -> {}
      }
    }
  }

  private static String nodeTag(GenomeView g, int id) {
    for (NodeView n : g.nodes()) {
      if (n.id() == id) {
        return switch (n.type()) {
          case "INPUT" -> "i" + id;
          case "BIAS" -> "b" + id;
          case "OUTPUT" -> "o" + id;
          default -> "h" + id;
        };
      }
    }
    return "n" + id;
  }

  private static Color nodeColor(String type) {
    return switch (type) {
      case "INPUT" -> Theme.NODE_INPUT;
      case "BIAS" -> Theme.NODE_BIAS;
      case "OUTPUT" -> Theme.NODE_OUTPUT;
      default -> Theme.NODE_HIDDEN;
    };
  }

  public int pickNode(float mx, float my, float radius) {
    int best = -1;
    float bestD = radius * radius;
    for (Map.Entry<Integer, float[]> e : pos.entrySet()) {
      float dx = e.getValue()[0] - mx;
      float dy = e.getValue()[1] - my;
      float d2 = dx * dx + dy * dy;
      if (d2 < bestD) {
        bestD = d2;
        best = e.getKey();
      }
    }
    return best;
  }
}
