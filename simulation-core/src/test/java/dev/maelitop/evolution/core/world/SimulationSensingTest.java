package dev.maelitop.evolution.core.world;

import static org.assertj.core.api.Assertions.assertThat;

import dev.maelitop.evolution.core.domain.Creature;
import dev.maelitop.evolution.core.domain.Entity;
import dev.maelitop.evolution.core.domain.Food;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.Vec2;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.neural.Activation;
import dev.maelitop.evolution.core.neural.Genome;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SimulationSensingTest {

  private static final Vec2 CENTER = new Vec2(1000, 1000);
  private static final Random RNG = new Random(1L);

  private final WorldConfig config = WorldConfig.defaults();
  private final SimulationEngine engine = new SimulationEngine(config, RNG);

  @Test
  void classifiesAllyInLeftEyeAndEnemyInRightEye() {
    Creature self = creature(Team.HERBIVORE, CENTER);
    Creature ally = creature(Team.HERBIVORE, new Vec2(1100, 990));
    Creature enemy = creature(Team.CARNIVORE, new Vec2(1100, 1010));

    double[] inputs = engine.sense(self, index(self, ally, enemy));

    assertThat(inputs[3]).isEqualTo(1.0);
    assertThat(inputs[2]).isZero();
    assertThat(inputs[4]).isZero();
    assertThat(inputs[1]).isNegative();
    assertThat(inputs[9]).isEqualTo(1.0);
    assertThat(inputs[7]).isZero();
    assertThat(inputs[6]).isPositive();
    assertThat(inputs[10]).isEqualTo(1.0);
  }

  @Test
  void reportsMaxDistanceForEmptyEyeAndDetectsFood() {
    Creature self = creature(Team.HERBIVORE, CENTER);
    Food food = new Food(99, new Vec2(1100, 1010));

    double[] inputs = engine.sense(self, index(self, food));

    assertThat(inputs[0]).isEqualTo(1.0);
    assertThat(inputs[7]).isEqualTo(1.0);
    assertThat(inputs[5]).isLessThan(1.0);
  }

  private Creature creature(Team team, Vec2 position) {
    Genome genome =
        Genome.initial(
            SimulationEngine.INPUT_COUNT,
            List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID),
            RNG);
    return new Creature(1, team, genome, position, 0, config.maxEnergy());
  }

  private Quadtree<Entity> index(Entity... entities) {
    Quadtree<Entity> index =
        new Quadtree<>(new Rectangle(0, 0, config.width() + 1.0, config.height() + 1.0), 8, 8);
    for (Entity entity : entities) {
      index.insert(entity.position().x(), entity.position().y(), entity);
    }
    return index;
  }
}
