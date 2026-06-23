plugins { `java-library` }

dependencies {
  api(libs.jackson.databind)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testRuntimeOnly(libs.junit.platform.launcher)
}
