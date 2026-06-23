package dev.maelitop.evolution.persistence;

import dev.maelitop.evolution.core.neural.Genome;

public record StoredAgent(long id, long generationId, double fitness, Genome genome) {}
