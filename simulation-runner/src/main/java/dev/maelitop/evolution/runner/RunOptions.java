package dev.maelitop.evolution.runner;

public record RunOptions(long seed, int generations) {

  public RunOptions {
    if (generations <= 0) {
      throw new IllegalArgumentException("generations must be positive");
    }
  }

  public static RunOptions parse(String[] args) {
    long seed = 0;
    int generations = 100;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--seed" -> seed = Long.parseLong(requireValue(args, ++i, "--seed"));
        case "--generations" ->
            generations = Integer.parseInt(requireValue(args, ++i, "--generations"));
        default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
      }
    }
    return new RunOptions(seed, generations);
  }

  private static String requireValue(String[] args, int index, String flag) {
    if (index >= args.length) {
      throw new IllegalArgumentException("missing value for " + flag);
    }
    return args[index];
  }
}
