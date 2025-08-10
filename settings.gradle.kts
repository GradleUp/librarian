pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()

      exclusiveContent {
        forRepository { maven("https://jitpack.io") }
        filter {
          includeGroup("com.github.kotlin-inquirer")
        }
      }
    }
  }
  repositories {
    maven("https://storage.googleapis.com/gradleup/m2") {
      content {
        includeGroup("com.gradleup.librarian")
        includeGroup("com.gradleup.compat.patrouille")
      }
    }
  }
}

include(":librarian-gradle-plugin", ":librarian-cli", ":librarian-core")
