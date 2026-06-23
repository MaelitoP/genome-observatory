package dev.maelitop.evolution.core.evolution;

public record GenerationStats(
    int generation, double bestFitness, double meanFitness, int population) {}
