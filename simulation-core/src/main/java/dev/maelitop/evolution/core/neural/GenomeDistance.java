package dev.maelitop.evolution.core.neural;

@FunctionalInterface
public interface GenomeDistance {

  double between(Genome a, Genome b);
}
