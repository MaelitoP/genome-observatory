package dev.maelitop.evolution.core.neural;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class InnovationTrackerTest {

  @Test
  void connectionInnovationsAreStableAndMonotonic() {
    InnovationTracker tracker = new InnovationTracker(45, 18);

    assertThat(tracker.connection(0, 5)).isEqualTo(45);
    assertThat(tracker.connection(0, 5)).isEqualTo(45);
    assertThat(tracker.connection(1, 5)).isEqualTo(46);
  }

  @Test
  void nodeSplitsAreStableKeyedByConnection() {
    InnovationTracker tracker = new InnovationTracker(45, 18);

    InnovationTracker.NodeSplit split = tracker.split(10);

    assertThat(split).isEqualTo(new InnovationTracker.NodeSplit(18, 45, 46));
    assertThat(tracker.split(10)).isEqualTo(split);
    assertThat(tracker.split(11)).isEqualTo(new InnovationTracker.NodeSplit(19, 47, 48));
  }

  @Test
  void startingAfterContinuesPastSeedGenome() {
    Genome seed =
        Genome.initial(
            14, List.of(Activation.TANH, Activation.TANH, Activation.SIGMOID), new Random(1L));

    InnovationTracker tracker = InnovationTracker.startingAfter(seed);

    assertThat(tracker.split(0)).isEqualTo(new InnovationTracker.NodeSplit(18, 45, 46));
  }
}
