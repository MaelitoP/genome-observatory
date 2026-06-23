package dev.maelitop.evolution.persistence;

import dev.maelitop.evolution.core.domain.WorldConfig;

public record StoredRun(
    long id, long seed, WorldConfig config, int generations, long startedAtEpochMs) {}
