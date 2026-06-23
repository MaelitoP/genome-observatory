package dev.maelitop.evolution.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ProtocolJson {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ProtocolJson() {}

  public static ObjectMapper mapper() {
    return MAPPER;
  }
}
