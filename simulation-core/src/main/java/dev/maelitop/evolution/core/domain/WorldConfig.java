package dev.maelitop.evolution.core.domain;

public record WorldConfig(
    int width,
    int height,
    int tickRate,
    int foodTarget,
    double foodEnergy,
    double foodRespawnPerSecond,
    int population,
    double generationSeconds,
    double maxEnergy,
    double metabolicCost,
    double moveCost,
    double spikeCost,
    double maxAgeSeconds,
    double maxSpeed,
    double maxTurnRate,
    double visionRange,
    double fieldOfView,
    double spikeMaxLength,
    double spikeDamagePerSecond,
    double carnivoreEnergyRecovery,
    FitnessWeights fitness) {

  public WorldConfig {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("world dimensions must be positive");
    }
    if (tickRate <= 0) {
      throw new IllegalArgumentException("tickRate must be positive");
    }
    if (population <= 0) {
      throw new IllegalArgumentException("population must be positive");
    }
    if (generationSeconds <= 0 || maxAgeSeconds <= 0) {
      throw new IllegalArgumentException("generation and age durations must be positive");
    }
    if (maxEnergy <= 0 || maxSpeed <= 0 || maxTurnRate <= 0) {
      throw new IllegalArgumentException("maxEnergy, maxSpeed and maxTurnRate must be positive");
    }
    if (visionRange <= 0 || fieldOfView <= 0 || spikeMaxLength <= 0) {
      throw new IllegalArgumentException(
          "visionRange, fieldOfView and spikeMaxLength must be positive");
    }
    if (spikeDamagePerSecond < 0) {
      throw new IllegalArgumentException("spikeDamagePerSecond must not be negative");
    }
    if (carnivoreEnergyRecovery < 0 || carnivoreEnergyRecovery > 1) {
      throw new IllegalArgumentException("carnivoreEnergyRecovery must be within [0, 1]");
    }
  }

  public static WorldConfig defaults() {
    return new WorldConfig(
        2000,
        2000,
        60,
        300,
        25.0,
        5.0,
        50,
        60.0,
        100.0,
        0.1,
        0.05,
        0.05,
        60.0,
        120.0,
        Math.PI,
        150.0,
        Math.toRadians(120),
        40.0,
        20.0,
        0.5,
        new FitnessWeights(1.0, 3.0, 0.1, 0.0));
  }
}
