package dev.maelitop.evolution.core.domain;

public sealed interface Entity permits Food, Creature {

  long id();

  Vec2 position();
}
