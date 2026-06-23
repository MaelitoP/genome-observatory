package dev.maelitop.evolution.protocol;

import java.util.List;

public record CreatureSnapshot(
    long id,
    double x,
    double y,
    double angle,
    Team team,
    double energy,
    double age,
    double spike,
    Color color,
    int generation,
    List<Long> looksAt)
    implements EntitySnapshot {

  public CreatureSnapshot {
    looksAt = List.copyOf(looksAt);
  }

  @Override
  public EntityKind kind() {
    return EntityKind.CREATURE;
  }
}
