package dev.maelitop.evolution.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.maelitop.evolution.protocol.RunStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
record RunSummary(
    long id,
    long seed,
    int generations,
    int carnivores,
    long startedAtEpochMs,
    RunStatus status,
    int currentGeneration) {

  RunSummary {
    status = status == null ? RunStatus.INTERRUPTED : status;
  }
}
