package dev.maelitop.evolution.persistence;

import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.evolution.GenerationStats;

public record GenerationRecord(Team team, GenerationStats stats) {}
