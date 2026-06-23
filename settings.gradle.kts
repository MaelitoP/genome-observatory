rootProject.name = "genetic-evolution"

dependencyResolutionManagement {
  repositories { mavenCentral() }
}

include("protocol", "simulation-core", "server", "client")
