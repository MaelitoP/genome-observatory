package dev.maelitop.evolution.client;

import dev.maelitop.evolution.protocol.RunStatus;
import dev.maelitop.evolution.protocol.Team;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Shared run-selection state (selection, team, compare set, launch form) across analytics views.
 */
public final class RunBrowser {

  private final RunApiClient api;
  private final Set<Long> compared = new LinkedHashSet<>();
  private final StartRunForm form = new StartRunForm();

  private int selected;
  private Team team = Team.HERBIVORE;
  private long loadedRun = -1;
  private Team loadedTeam;

  public RunBrowser(RunApiClient api) {
    this.api = api;
  }

  public List<RunSummary> runs() {
    return api.runs();
  }

  public Team team() {
    return team;
  }

  public Set<Long> compared() {
    return compared;
  }

  public StartRunForm form() {
    return form;
  }

  public int selectedIndex() {
    return clamp(selected);
  }

  public Optional<RunSummary> selectedRun() {
    List<RunSummary> runs = runs();
    return runs.isEmpty() ? Optional.empty() : Optional.of(runs.get(clamp(selected)));
  }

  public long selectedRunId() {
    return selectedRun().map(RunSummary::id).orElse(-1L);
  }

  public Optional<GenomeView> champion() {
    return api.champion();
  }

  public List<GenerationRecordView.Stats> teamPoints(long runId) {
    List<GenerationRecordView.Stats> points = new ArrayList<>();
    for (GenerationRecordView record : api.generations(runId)) {
      if (record.team() == team) {
        points.add(record.stats());
      }
    }
    return points;
  }

  public List<GenerationRecordView.Stats> bestPoints(long runId, Team forTeam) {
    List<GenerationRecordView.Stats> points = new ArrayList<>();
    for (GenerationRecordView record : api.generations(runId)) {
      if (record.team() == forTeam) {
        points.add(record.stats());
      }
    }
    return points;
  }

  public void refresh() {
    loadedRun = -1;
    var _ = api.refreshRuns();
  }

  public void ensureRunsLoaded() {
    if (runs().isEmpty()) {
      var _ = api.refreshRuns();
    }
  }

  /**
   * Periodic refresh: re-fetch the run list, and the selected run's curve while it is computing.
   */
  public void poll() {
    var _ = api.refreshRuns();
    selectedRun()
        .filter(run -> run.status() == RunStatus.RUNNING)
        .ifPresent(
            run -> {
              var _ = api.loadGenerations(run.id());
            });
  }

  /** How many non-terminal runs are queued ahead of this one (executor processes in id order). */
  public int aheadOf(RunSummary run) {
    int ahead = 0;
    for (RunSummary other : runs()) {
      if (other.id() < run.id() && !other.status().terminal()) {
        ahead++;
      }
    }
    return ahead;
  }

  /** Loads generations + champion for the current selection when it changes. */
  public void sync() {
    List<RunSummary> runs = runs();
    if (runs.isEmpty()) {
      return;
    }
    selected = clamp(selected);
    long id = runs.get(selected).id();
    if (id != loadedRun || team != loadedTeam) {
      loadedRun = id;
      loadedTeam = team;
      var _ = api.loadGenerations(id);
      var _ = api.loadChampion(id, team);
    }
  }

  public void selectPrev() {
    selected = clamp(selected - 1);
  }

  public void selectNext() {
    selected = clamp(selected + 1);
  }

  public void select(int index) {
    selected = clamp(index);
  }

  public void toggleTeam() {
    team = team == Team.HERBIVORE ? Team.CARNIVORE : Team.HERBIVORE;
    loadedTeam = null;
  }

  public void toggleCompareSelected() {
    long id = selectedRunId();
    if (id < 0) {
      return;
    }
    if (!compared.remove(id)) {
      compared.add(id);
      var _ = api.loadGenerations(id);
    }
  }

  public void clearCompare() {
    compared.clear();
  }

  public void startRun() {
    api.startRun(form.seed(), form.generations(), form.carnivores());
    form.close();
  }

  public void exportChampion() {
    api.exportChampion(Path.of("champion-" + selectedRunId() + "-" + team + ".json"));
  }

  private int clamp(int index) {
    int size = runs().size();
    return size == 0 ? 0 : Math.clamp(index, 0, size - 1);
  }
}
