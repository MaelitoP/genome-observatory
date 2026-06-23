package dev.maelitop.evolution.protocol;

public record GenerationSummary(
    int generation, double bestFitness, double meanFitness, double diversity, long championAgentId)
    implements ServerMessage {}
