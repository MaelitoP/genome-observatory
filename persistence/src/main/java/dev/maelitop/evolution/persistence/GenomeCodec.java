package dev.maelitop.evolution.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.neural.Genome;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class GenomeCodec {

  private final ObjectMapper mapper;

  public GenomeCodec(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  public String toJson(Genome genome) {
    try {
      return mapper.writeValueAsString(genome);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException("failed to serialize genome", e);
    }
  }

  public Genome fromJson(String json) {
    try {
      return mapper.readValue(json, Genome.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("failed to parse genome", e);
    }
  }

  public void write(Genome genome, Path path) {
    try {
      Files.writeString(path, toJson(genome));
    } catch (IOException e) {
      throw new UncheckedIOException("failed to write genome to " + path, e);
    }
  }
}
