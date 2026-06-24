package dev.maelitop.evolution.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.maelitop.evolution.protocol.Team;

@JsonIgnoreProperties(ignoreUnknown = true)
record GenerationRecordView(Team team, Stats stats) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Stats(
      int generation,
      double bestFitness,
      double meanFitness,
      double medianFitness,
      double diversity,
      int population) {}
}
