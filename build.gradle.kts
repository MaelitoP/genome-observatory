plugins { id("com.diffplug.spotless") version "7.0.2" apply false }

subprojects {
  apply(plugin = "java")
  apply(plugin = "com.diffplug.spotless")

  repositories { mavenCentral() }

  extensions.configure<JavaPluginExtension> {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
  }

  extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
      googleJavaFormat("1.27.0")
      removeUnusedImports()
      trimTrailingWhitespace()
      endWithNewline()
    }
  }

  tasks.withType<Test>().configureEach { useJUnitPlatform() }
}
