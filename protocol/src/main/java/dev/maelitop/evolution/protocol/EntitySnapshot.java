package dev.maelitop.evolution.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
  @JsonSubTypes.Type(value = FoodSnapshot.class, name = "FOOD"),
  @JsonSubTypes.Type(value = CreatureSnapshot.class, name = "CREATURE")
})
public sealed interface EntitySnapshot permits FoodSnapshot, CreatureSnapshot {
  long id();

  EntityKind kind();

  double x();

  double y();
}
