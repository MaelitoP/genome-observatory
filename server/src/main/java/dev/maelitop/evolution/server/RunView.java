package dev.maelitop.evolution.server;

import dev.maelitop.evolution.persistence.StoredRun;
import dev.maelitop.evolution.protocol.RunStatus;

/** API view of a run: the stored fields plus the derived live progress. */
public record RunView(
    long id,
    long seed,
    int generations,
    int carnivores,
    long startedAtEpochMs,
    RunStatus status,
    int currentGeneration) {

  static RunView of(StoredRun run, int currentGeneration) {
    return new RunView(
        run.id(),
        run.seed(),
        run.generations(),
        run.carnivores(),
        run.startedAtEpochMs(),
        run.status(),
        currentGeneration);
  }
}
