package dev.maelitop.evolution.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record GenomeView(List<NodeView> nodes, List<ConnectionView> connections) {

  GenomeView {
    nodes = List.copyOf(nodes);
    connections = List.copyOf(connections);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record NodeView(int id, String type) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ConnectionView(int in, int out, double weight, boolean enabled) {}
}
