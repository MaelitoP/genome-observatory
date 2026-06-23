package dev.maelitop.evolution.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.Genome;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenomeCodecTest {

  private static final List<Activation> OUTPUTS =
      List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID);

  private final GenomeCodec codec = new GenomeCodec(new ObjectMapper());

  @Test
  void roundTripsThroughJsonString() {
    Genome genome = genome(1L);
    assertThat(codec.fromJson(codec.toJson(genome))).isEqualTo(genome);
  }

  @Test
  void roundTripsThroughFile(@TempDir Path dir) throws IOException {
    Genome genome = genome(2L);
    Path file = dir.resolve("champion.json");

    codec.write(genome, file);

    assertThat(codec.fromJson(Files.readString(file))).isEqualTo(genome);
  }

  @Test
  void rejectsMalformedJson() {
    assertThatThrownBy(() -> codec.fromJson("{not json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("failed to parse");
  }

  private static Genome genome(long seed) {
    return Genome.initial(14, OUTPUTS, RandomGeneratorFactory.of("L64X128MixRandom").create(seed));
  }
}
