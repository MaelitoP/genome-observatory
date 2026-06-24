package dev.maelitop.evolution.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.protocol.Team;
import org.junit.jupiter.api.Test;

class ViewRecordJsonTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void runSummaryIgnoresNestedConfig() throws Exception {
    String json =
        "[{\"id\":1,\"seed\":42,\"config\":{\"width\":2000,\"height\":2000,\"tickRate\":60},"
            + "\"generations\":100,\"carnivores\":3,\"startedAtEpochMs\":1700000000}]";

    RunSummary[] runs = mapper.readValue(json, RunSummary[].class);

    assertThat(runs).hasSize(1);
    assertThat(runs[0].id()).isEqualTo(1L);
    assertThat(runs[0].generations()).isEqualTo(100);
    assertThat(runs[0].carnivores()).isEqualTo(3);
  }

  @Test
  void generationRecordKeepsNestedStats() throws Exception {
    String json =
        "{\"team\":\"HERBIVORE\",\"stats\":{\"generation\":4,\"bestFitness\":12.5,"
            + "\"meanFitness\":6.0,\"medianFitness\":5.5,\"diversity\":0.3,\"population\":50}}";

    GenerationRecordView record = mapper.readValue(json, GenerationRecordView.class);

    assertThat(record.team()).isEqualTo(Team.HERBIVORE);
    assertThat(record.stats().bestFitness()).isEqualTo(12.5);
    assertThat(record.stats().diversity()).isEqualTo(0.3);
  }

  @Test
  void genomeViewIgnoresActivationAndInnovation() throws Exception {
    String json =
        "{\"nodes\":[{\"id\":0,\"type\":\"INPUT\",\"activation\":\"IDENTITY\"},"
            + "{\"id\":15,\"type\":\"OUTPUT\",\"activation\":\"TANH\"}],"
            + "\"connections\":[{\"in\":0,\"out\":15,\"weight\":0.5,\"enabled\":true,"
            + "\"innovation\":7}]}";

    GenomeView genome = mapper.readValue(json, GenomeView.class);

    assertThat(genome.nodes()).hasSize(2);
    assertThat(genome.nodes().get(1).type()).isEqualTo("OUTPUT");
    assertThat(genome.connections()).hasSize(1);
    assertThat(genome.connections().get(0).weight()).isEqualTo(0.5);
    assertThat(genome.connections().get(0).enabled()).isTrue();
  }
}
