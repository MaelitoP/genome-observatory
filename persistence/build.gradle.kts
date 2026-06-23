plugins { `java-library` }

dependencies {
  api(project(":simulation-core"))
  api(libs.jackson.databind)
  implementation(libs.slf4j.api)
  runtimeOnly(libs.sqlite.jdbc)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testRuntimeOnly(libs.junit.platform.launcher)
  testRuntimeOnly(libs.logback.classic)
}
