package dev.maelitop.evolution.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Pause.class, name = "PAUSE"),
  @JsonSubTypes.Type(value = Resume.class, name = "RESUME"),
  @JsonSubTypes.Type(value = Step.class, name = "STEP"),
  @JsonSubTypes.Type(value = SetSpeed.class, name = "SET_SPEED"),
  @JsonSubTypes.Type(value = SetParams.class, name = "SET_PARAMS"),
  @JsonSubTypes.Type(value = WatchAgent.class, name = "WATCH_AGENT")
})
public sealed interface ClientMessage
    permits Pause, Resume, Step, SetSpeed, SetParams, WatchAgent {}
