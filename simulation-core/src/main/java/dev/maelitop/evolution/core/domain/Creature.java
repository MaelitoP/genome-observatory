package dev.maelitop.evolution.core.domain;

import dev.maelitop.evolution.core.neural.Genome;
import dev.maelitop.evolution.core.neural.NetworkPhenotype;
import java.util.HashSet;
import java.util.Set;

public final class Creature implements Entity {

  private static final double EXPLORE_CELL_SIZE = 50.0;

  private final long id;
  private final Team team;
  private final Genome genome;
  private final NetworkPhenotype brain;

  private Vec2 position;
  private double heading;
  private double speed;
  private double energy;
  private double age;
  private double spikeLength;
  private boolean alive = true;

  private long survivalTicks;
  private double energyGained;
  private final Set<Long> visitedCells = new HashSet<>();

  public Creature(long id, Team team, Genome genome, Vec2 position, double heading, double energy) {
    this.id = id;
    this.team = team;
    this.genome = genome;
    this.brain = genome.express();
    this.position = position;
    this.heading = heading;
    this.energy = energy;
    this.visitedCells.add(cellOf(position));
  }

  @Override
  public long id() {
    return id;
  }

  @Override
  public Vec2 position() {
    return position;
  }

  public Team team() {
    return team;
  }

  public Genome genome() {
    return genome;
  }

  public NetworkPhenotype brain() {
    return brain;
  }

  public double heading() {
    return heading;
  }

  public double speed() {
    return speed;
  }

  public double energy() {
    return energy;
  }

  public double age() {
    return age;
  }

  public double spikeLength() {
    return spikeLength;
  }

  public boolean alive() {
    return alive;
  }

  public long survivalTicks() {
    return survivalTicks;
  }

  public double energyGained() {
    return energyGained;
  }

  public int uniqueCellsVisited() {
    return visitedCells.size();
  }

  public void act(double turn, double thrust, double spike, WorldConfig config, double dt) {
    heading += Math.clamp(turn, -1, 1) * config.maxTurnRate() * dt;
    speed = Math.clamp(thrust, -1, 1) * config.maxSpeed();
    double nx = Math.clamp(position.x() + Math.cos(heading) * speed * dt, 0, config.width());
    double ny = Math.clamp(position.y() + Math.sin(heading) * speed * dt, 0, config.height());
    position = new Vec2(nx, ny);
    spikeLength = Math.clamp(spike, 0, 1) * config.spikeMaxLength();

    energy -=
        config.metabolicCost()
            + config.moveCost() * Math.abs(speed) / config.maxSpeed()
            + config.spikeCost() * spikeLength / config.spikeMaxLength();
    age += dt;
    survivalTicks++;
    visitedCells.add(cellOf(position));
  }

  public void eat(double amount, double maxEnergy) {
    double before = energy;
    energy = Math.min(maxEnergy, energy + amount);
    energyGained += energy - before;
  }

  public void die() {
    alive = false;
  }

  private static long cellOf(Vec2 position) {
    long col = (long) Math.floor(position.x() / EXPLORE_CELL_SIZE);
    long row = (long) Math.floor(position.y() / EXPLORE_CELL_SIZE);
    return (col << 32) ^ (row & 0xffffffffL);
  }
}
