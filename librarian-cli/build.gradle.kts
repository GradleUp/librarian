import com.gradleup.librarian.core.librarianModule

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("distribution")
}

librarianModule()

dependencies {
  implementation(project(":librarian-gradle-plugin"))
  implementation(project(":librarian-core"))
  implementation(libs.clikt)
  implementation(libs.inquirer)
  implementation(libs.jsonpath)
  implementation(libs.jansi)
  implementation(libs.okhttp)
  implementation(libs.serialization.json)
  testImplementation(kotlin("test"))
}

val startScriptTaskProvider =
  tasks.register("createStartScript", org.gradle.jvm.application.tasks.CreateStartScripts::class.java) {
    outputDir = file("build/start_scripts/")
    mainClass.set("com.gradleup.librarian.cli.MainKt")
    applicationName = "librarian"
    classpath = files(configurations["runtimeClasspath"], tasks.named("jar").map { it.outputs.files.files })
  }

distributions.named("main").configure {
  distributionBaseName = "librarian"
  contents {
    from(configurations["runtimeClasspath"]) {
      into("lib")
    }
    from(tasks.named("jar")) {
      into("lib")
    }
    from(startScriptTaskProvider) {
      into("bin")
    }
  }
}
