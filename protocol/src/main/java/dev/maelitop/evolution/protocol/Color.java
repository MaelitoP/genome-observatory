package dev.maelitop.evolution.protocol;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record Color(float r, float g, float b) {}
