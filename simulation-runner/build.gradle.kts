plugins { application }

dependencies {
  implementation(project(":simulation-core"))
  implementation(project(":persistence"))
  implementation(libs.slf4j.api)
  runtimeOnly(libs.logback.classic)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testRuntimeOnly(libs.junit.platform.launcher)
}

application { mainClass.set("dev.maelitop.evolution.runner.Main") }
