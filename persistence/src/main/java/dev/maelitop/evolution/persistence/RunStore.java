package dev.maelitop.evolution.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maelitop.evolution.core.domain.Team;
import dev.maelitop.evolution.core.domain.WorldConfig;
import dev.maelitop.evolution.core.evolution.Evaluated;
import dev.maelitop.evolution.core.evolution.GenerationStats;
import dev.maelitop.evolution.core.neural.Genome;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RunStore implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(RunStore.class);

  private final Connection connection;
  private final ObjectMapper mapper;
  private final GenomeCodec genomes;

  public RunStore(String jdbcUrl, ObjectMapper mapper) {
    Objects.requireNonNull(jdbcUrl, "jdbcUrl");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.genomes = new GenomeCodec(mapper);
    try {
      this.connection = DriverManager.getConnection(jdbcUrl);
      initSchema();
    } catch (SQLException e) {
      throw new IllegalStateException("failed to open run store: " + jdbcUrl, e);
    }
  }

  private void initSchema() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
      statement.executeUpdate(
          "CREATE TABLE IF NOT EXISTS run ("
              + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "seed INTEGER NOT NULL,"
              + "config TEXT NOT NULL,"
              + "generations INTEGER NOT NULL,"
              + "carnivores INTEGER NOT NULL DEFAULT 0,"
              + "started_at INTEGER NOT NULL)");
      statement.executeUpdate(
          "CREATE TABLE IF NOT EXISTS generation ("
              + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "run_id INTEGER NOT NULL REFERENCES run(id),"
              + "idx INTEGER NOT NULL,"
              + "team TEXT NOT NULL DEFAULT 'HERBIVORE',"
              + "best_fitness REAL NOT NULL,"
              + "mean_fitness REAL NOT NULL,"
              + "median_fitness REAL NOT NULL,"
              + "diversity REAL NOT NULL,"
              + "population INTEGER NOT NULL)");
      statement.executeUpdate(
          "CREATE TABLE IF NOT EXISTS agent ("
              + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "generation_id INTEGER NOT NULL REFERENCES generation(id),"
              + "genome_json TEXT NOT NULL,"
              + "fitness REAL NOT NULL)");
    }
    ensureColumn("run", "carnivores", "carnivores INTEGER NOT NULL DEFAULT 0");
    ensureColumn("generation", "team", "team TEXT NOT NULL DEFAULT 'HERBIVORE'");
  }

  private void ensureColumn(String table, String column, String columnDdl) {
    try {
      if (hasColumn(table, column)) {
        return;
      }
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + columnDdl);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("failed to add column " + table + "." + column, e);
    }
  }

  private boolean hasColumn(String table, String column) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
      while (rs.next()) {
        if (column.equals(rs.getString("name"))) {
          return true;
        }
      }
      return false;
    }
  }

  public long startRun(RunSpec spec) {
    String sql =
        "INSERT INTO run(seed, config, generations, carnivores, started_at) VALUES (?,?,?,?,?)";
    try (PreparedStatement statement =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setLong(1, spec.seed());
      statement.setString(2, mapper.writeValueAsString(spec.config()));
      statement.setInt(3, spec.generations());
      statement.setInt(4, spec.carnivores());
      statement.setLong(5, spec.startedAtEpochMs());
      statement.executeUpdate();
      return generatedKey(statement);
    } catch (SQLException | JsonProcessingException e) {
      throw new IllegalStateException("failed to start run", e);
    }
  }

  public void recordGeneration(
      long runId, Team team, GenerationStats stats, List<Evaluated> population) {
    try {
      connection.setAutoCommit(false);
      long generationId = insertGeneration(runId, team, stats);
      insertAgents(generationId, population);
      connection.commit();
    } catch (SQLException e) {
      rollbackQuietly(e);
      throw new IllegalStateException("failed to record generation " + stats.generation(), e);
    } finally {
      restoreAutoCommit();
    }
  }

  private long insertGeneration(long runId, Team team, GenerationStats stats) throws SQLException {
    String sql =
        "INSERT INTO generation("
            + "run_id, idx, team, best_fitness, mean_fitness, median_fitness, diversity, population)"
            + " VALUES (?,?,?,?,?,?,?,?)";
    try (PreparedStatement statement =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      statement.setLong(1, runId);
      statement.setInt(2, stats.generation());
      statement.setString(3, team.name());
      statement.setDouble(4, stats.bestFitness());
      statement.setDouble(5, stats.meanFitness());
      statement.setDouble(6, stats.medianFitness());
      statement.setDouble(7, stats.diversity());
      statement.setInt(8, stats.population());
      statement.executeUpdate();
      return generatedKey(statement);
    }
  }

  private void insertAgents(long generationId, List<Evaluated> population) throws SQLException {
    String sql = "INSERT INTO agent(generation_id, genome_json, fitness) VALUES (?,?,?)";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (Evaluated agent : population) {
        statement.setLong(1, generationId);
        statement.setString(2, genomes.toJson(agent.genome()));
        statement.setDouble(3, agent.fitness());
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  public Optional<StoredRun> loadRun(long id) {
    String sql = "SELECT seed, config, generations, carnivores, started_at FROM run WHERE id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }
        return Optional.of(readRun(id, rs));
      }
    } catch (SQLException | JsonProcessingException e) {
      throw new IllegalStateException("failed to load run " + id, e);
    }
  }

  public List<StoredRun> listRuns() {
    String sql =
        "SELECT id, seed, config, generations, carnivores, started_at FROM run ORDER BY id";
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet rs = statement.executeQuery()) {
      List<StoredRun> runs = new ArrayList<>();
      while (rs.next()) {
        runs.add(readRun(rs.getLong("id"), rs));
      }
      return runs;
    } catch (SQLException | JsonProcessingException e) {
      throw new IllegalStateException("failed to list runs", e);
    }
  }

  private StoredRun readRun(long id, ResultSet rs) throws SQLException, JsonProcessingException {
    WorldConfig config = mapper.readValue(rs.getString("config"), WorldConfig.class);
    return new StoredRun(
        id,
        rs.getLong("seed"),
        config,
        rs.getInt("generations"),
        rs.getInt("carnivores"),
        rs.getLong("started_at"));
  }

  public Optional<StoredAgent> loadAgent(long agentId) {
    String sql =
        "SELECT a.generation_id, a.genome_json, a.fitness, g.team FROM agent a"
            + " JOIN generation g ON a.generation_id = g.id WHERE a.id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, agentId);
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }
        return Optional.of(
            new StoredAgent(
                agentId,
                rs.getLong("generation_id"),
                readTeam(rs),
                rs.getDouble("fitness"),
                genomes.fromJson(rs.getString("genome_json"))));
      }
    } catch (SQLException e) {
      throw new IllegalStateException("failed to load agent " + agentId, e);
    }
  }

  public List<GenerationStats> loadGenerations(long runId, Team team) {
    String sql =
        "SELECT idx, best_fitness, mean_fitness, median_fitness, diversity, population"
            + " FROM generation WHERE run_id = ? AND team = ? ORDER BY idx";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, runId);
      statement.setString(2, team.name());
      try (ResultSet rs = statement.executeQuery()) {
        List<GenerationStats> generations = new ArrayList<>();
        while (rs.next()) {
          generations.add(readStats(rs));
        }
        return generations;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("failed to load generations for run " + runId, e);
    }
  }

  public List<GenerationRecord> loadGenerationRecords(long runId) {
    String sql =
        "SELECT team, idx, best_fitness, mean_fitness, median_fitness, diversity, population"
            + " FROM generation WHERE run_id = ? ORDER BY idx, team";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, runId);
      try (ResultSet rs = statement.executeQuery()) {
        List<GenerationRecord> records = new ArrayList<>();
        while (rs.next()) {
          records.add(new GenerationRecord(readTeam(rs), readStats(rs)));
        }
        return records;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("failed to load generation records for run " + runId, e);
    }
  }

  private static Team readTeam(ResultSet rs) throws SQLException {
    String raw = rs.getString("team");
    try {
      return Team.valueOf(raw);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("unknown team in database: " + raw, e);
    }
  }

  private static GenerationStats readStats(ResultSet rs) throws SQLException {
    return new GenerationStats(
        rs.getInt("idx"),
        rs.getDouble("best_fitness"),
        rs.getDouble("mean_fitness"),
        rs.getDouble("median_fitness"),
        rs.getDouble("diversity"),
        rs.getInt("population"));
  }

  public Optional<Genome> loadChampion(long runId, Team team) {
    String sql =
        "SELECT a.genome_json FROM agent a"
            + " JOIN generation g ON a.generation_id = g.id"
            + " WHERE g.run_id = ? AND g.team = ? ORDER BY a.fitness DESC, a.id ASC LIMIT 1";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, runId);
      statement.setString(2, team.name());
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }
        return Optional.of(genomes.fromJson(rs.getString("genome_json")));
      }
    } catch (SQLException e) {
      throw new IllegalStateException("failed to load champion for run " + runId, e);
    }
  }

  public Optional<Genome> loadOverallChampion(long id) {
    String sql =
        "SELECT a.genome_json FROM agent a"
            + " JOIN generation g ON a.generation_id = g.id"
            + " WHERE g.run_id = ? ORDER BY a.fitness DESC, a.id ASC LIMIT 1";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }
        return Optional.of(genomes.fromJson(rs.getString("genome_json")));
      }
    } catch (SQLException e) {
      throw new IllegalStateException("failed to load champion for run " + id, e);
    }
  }

  private static long generatedKey(PreparedStatement statement) throws SQLException {
    try (ResultSet keys = statement.getGeneratedKeys()) {
      if (!keys.next()) {
        throw new IllegalStateException("insert did not return a generated key");
      }
      return keys.getLong(1);
    }
  }

  private void rollbackQuietly(SQLException primary) {
    try {
      connection.rollback();
    } catch (SQLException e) {
      primary.addSuppressed(e);
    }
  }

  private void restoreAutoCommit() {
    try {
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      log.warn("failed to restore auto-commit", e);
    }
  }

  @Override
  public void close() {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new IllegalStateException("failed to close run store", e);
    }
  }
}
