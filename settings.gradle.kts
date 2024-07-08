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
}

include(":librarian-gradle-plugin", ":librarian-cli", ":librarian-core")