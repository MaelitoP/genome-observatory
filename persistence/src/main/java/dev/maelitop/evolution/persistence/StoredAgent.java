package dev.maelitop.evolution.persistence;

import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.neural.Genome;

public record StoredAgent(long id, long generationId, Team team, double fitness, Genome genome) {}
