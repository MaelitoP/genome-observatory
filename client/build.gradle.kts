plugins { application }

dependencies {
  implementation(project(":protocol"))
  implementation(libs.gdx.core)
  implementation(libs.gdx.backend.lwjgl3)
  implementation(libs.gdx.freetype)
  runtimeOnly(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
  runtimeOnly(variantOf(libs.gdx.freetype.platform) { classifier("natives-desktop") })
  implementation(libs.java.websocket)
  implementation(libs.slf4j.api)
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

tasks.register<JavaExec>("capture") {
  group = "verification"
  description = "Captures each view against a running server to PNGs (pass -Pout=<dir>)."
  mainClass.set("dev.maelitop.evolution.client.preview.CaptureMain")
  classpath = sourceSets["main"].runtimeClasspath
  args((project.findProperty("out") ?: layout.buildDirectory.dir("capture").get().asFile.path))
  val extra = mutableListOf("--enable-native-access=ALL-UNNAMED")
  if (System.getProperty("os.name").lowercase().contains("mac")) {
    extra.add("-XstartOnFirstThread")
  }
  jvmArgs = extra
}
