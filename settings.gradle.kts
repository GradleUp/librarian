pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()
      mavenLocal()
      // custom repo for https://github.com/JakeWharton/mosaic/pull/1100
      // reminder to remove when not forking anymore
      maven("https://storage.googleapis.com/gradleup/m2") {
        content {
          includeGroup("com.gradleup.mosaic")
        }
      }
      maven("https://storage.googleapis.com/gradleup/m2") {
        content {
          // those dependencies are only used at build time, and is safe to fetch as a snapshot
          includeModule("com.gradleup.gratatouille", "gratatouille-processor")
          includeModule("com.gradleup.tapmoc", "tapmoc-tasks")
          includeModule("com.gradleup.nmcp", "nmcp-tasks")
        }
      }
    }
  }
  repositories {
    maven("https://storage.googleapis.com/gradleup/m2") {
      content {
        includeGroup("com.gradleup.librarian")
        includeGroup("com.gradleup.tapmoc")
        includeGroup("com.gradleup.nmcp")
      }
    }
  }
}

include(":librarian-gradle-plugin", ":librarian-cli", ":librarian-core")
