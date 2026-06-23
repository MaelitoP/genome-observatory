package dev.maelitop.evolution.runner;

public record RunOptions(
    long seed, int generations, String dbPath, Long replayRunId, String exportChampionPath) {

  public RunOptions {
    if (generations <= 0) {
      throw new IllegalArgumentException("generations must be positive");
    }
    if (replayRunId != null && dbPath == null) {
      throw new IllegalArgumentException("--replay requires --db");
    }
    if (exportChampionPath != null && dbPath == null) {
      throw new IllegalArgumentException("--export-champion requires --db");
    }
  }

  public static RunOptions parse(String[] args) {
    long seed = 0;
    int generations = 100;
    String dbPath = null;
    Long replayRunId = null;
    String exportChampionPath = null;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--seed" -> seed = Long.parseLong(requireValue(args, ++i, "--seed"));
        case "--generations" ->
            generations = Integer.parseInt(requireValue(args, ++i, "--generations"));
        case "--db" -> dbPath = requireValue(args, ++i, "--db");
        case "--replay" -> replayRunId = Long.parseLong(requireValue(args, ++i, "--replay"));
        case "--export-champion" ->
            exportChampionPath = requireValue(args, ++i, "--export-champion");
        default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
      }
    }
    return new RunOptions(seed, generations, dbPath, replayRunId, exportChampionPath);
  }

  private static String requireValue(String[] args, int index, String flag) {
    if (index >= args.length) {
      throw new IllegalArgumentException("missing value for " + flag);
    }
    return args[index];
  }
}
