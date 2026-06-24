package dev.maelitop.evolution.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record RunSummary(long id, long seed, int generations, int carnivores, long startedAtEpochMs) {}
