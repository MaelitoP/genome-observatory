rootProject.name = "genome-observatory"

dependencyResolutionManagement {
  repositories { mavenCentral() }
}

include("protocol", "simulation-core", "simulation-runner", "persistence", "server", "client")
