plugins {
  `embedded-kotlin`
  id("java-gradle-plugin")
}

dependencies {
  implementation("com.gradleup.librarian:librarian-core:0.0.0")
}

gradlePlugin {
  plugins {
    create("build-logic.build-logic") {
      id = "build-logic.build-logic"
      implementationClass = "BuildLogicPlugin"
      description = "Unused"
    }
  }
}