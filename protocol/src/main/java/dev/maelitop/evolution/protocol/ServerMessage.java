package dev.maelitop.evolution.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public sealed interface ServerMessage permits HelloMessage, WorldSnapshot, GenerationSummary {

  static ServerMessage parse(ObjectMapper mapper, String json) throws JsonProcessingException {
    JsonNode node = mapper.readTree(json);
    if (node.has("entities")) {
      return mapper.treeToValue(node, WorldSnapshot.class);
    }
    if (node.has("runId")) {
      return mapper.treeToValue(node, HelloMessage.class);
    }
    if (node.has("championAgentId")) {
      return mapper.treeToValue(node, GenerationSummary.class);
    }
    throw new IllegalArgumentException("unrecognized server frame: " + node.fieldNames());
  }
}
