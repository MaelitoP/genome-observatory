package dev.maelitop.evolution.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldSnapshotJsonTest {

  private final ObjectMapper mapper = ProtocolJson.mapper();

  @Test
  void roundTripsSnapshotWithBothEntityKinds() throws Exception {
    WorldSnapshot original =
        new WorldSnapshot(
            42L,
            7,
            2,
            new WorldStats(1234.5, 410.2, 0.37),
            List.of(
                new FoodSnapshot(1L, 812.0, 410.0),
                new CreatureSnapshot(
                    2L,
                    120.5,
                    300.0,
                    47.0,
                    Team.HERBIVORE,
                    0.62,
                    0.30,
                    0.0,
                    new Color(0.9f, 0.3f, 0.2f),
                    7,
                    List.of(5L, 9L))));

    String json = mapper.writeValueAsString(original);
    WorldSnapshot restored = mapper.readValue(json, WorldSnapshot.class);

    assertThat(restored).isEqualTo(original);
  }

  @Test
  void serializesKindDiscriminatorAndColorAsArray() throws Exception {
    var node =
        mapper.readTree(
            mapper.writeValueAsString(
                new CreatureSnapshot(
                    2L,
                    1.0,
                    2.0,
                    0.0,
                    Team.HERBIVORE,
                    1.0,
                    0.0,
                    0.0,
                    new Color(0.1f, 0.2f, 0.3f),
                    0,
                    List.of())));

    assertThat(node.get("kind").asText()).isEqualTo("CREATURE");
    assertThat(node.get("color")).hasSize(3);
    assertThat(node.get("color").get(0).floatValue()).isEqualTo(0.1f);
  }

  @Test
  void deserializesEntityToConcreteSealedType() throws Exception {
    String json = "{\"id\":1,\"kind\":\"FOOD\",\"x\":5.0,\"y\":6.0}";

    EntitySnapshot entity = mapper.readValue(json, EntitySnapshot.class);

    assertThat(entity).isInstanceOf(FoodSnapshot.class);
    assertThat(entity.kind()).isEqualTo(EntityKind.FOOD);
  }
}
