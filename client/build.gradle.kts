plugins { application }

dependencies {
  implementation(project(":protocol"))
  implementation(libs.gdx.core)
  implementation(libs.gdx.backend.lwjgl3)
  runtimeOnly(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
  implementation(libs.java.websocket)
  runtimeOnly(libs.logback.classic)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testRuntimeOnly(libs.junit.platform.launcher)
}

application {
  mainClass.set("dev.maelitop.evolution.client.ClientMain")
  val jvmArgs = mutableListOf("--enable-native-access=ALL-UNNAMED")
  if (System.getProperty("os.name").lowercase().contains("mac")) {
    jvmArgs.add("-XstartOnFirstThread")
  }
  applicationDefaultJvmArgs = jvmArgs
}
