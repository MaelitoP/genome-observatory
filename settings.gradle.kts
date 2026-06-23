rootProject.name = "genetic-evolution"

dependencyResolutionManagement {
  repositories { mavenCentral() }
}

include("protocol", "simulation-core", "simulation-runner", "persistence", "server", "client")
