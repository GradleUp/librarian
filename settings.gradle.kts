pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()

      exclusiveContent {
        forRepository { maven("https://storage.googleapis.com/gradleup/m2") }
        filter {
          includeGroup("com.gradleup.librarian")
        }
      }

      exclusiveContent {
        forRepository { maven("https://jitpack.io") }
        filter {
          includeGroup("com.github.kotlin-inquirer")
        }
      }
    }
  }
}

include(":librarian-gradle-plugin", ":librarian-cli", ":librarian-core")

//includeBuild("../gratatouille")