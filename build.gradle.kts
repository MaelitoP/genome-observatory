import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("com.diffplug.spotless") version "7.0.2" apply false
  id("net.ltgt.errorprone") version "5.1.0" apply false
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "com.diffplug.spotless")
  apply(plugin = "net.ltgt.errorprone")

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

  dependencies { add("errorprone", "com.google.errorprone:error_prone_core:2.50.0") }

  tasks.withType<JavaCompile>().configureEach {
    options.errorprone { disableWarningsInGeneratedCode.set(true) }
  }

  tasks.withType<Test>().configureEach { useJUnitPlatform() }
}
