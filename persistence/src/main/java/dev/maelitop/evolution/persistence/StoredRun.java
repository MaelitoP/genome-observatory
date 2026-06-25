package dev.maelitop.evolution.persistence;

import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.protocol.RunStatus;

public record StoredRun(
    long id,
    long seed,
    WorldConfig config,
    int generations,
    int carnivores,
    long startedAtEpochMs,
    RunStatus status) {}
