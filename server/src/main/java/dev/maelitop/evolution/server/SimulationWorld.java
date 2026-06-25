package dev.maelitop.evolution.server;

import dev.maelitop.evolution.protocol.WorldSnapshot;

interface SimulationWorld {

  void step();

  WorldSnapshot snapshot();
}
