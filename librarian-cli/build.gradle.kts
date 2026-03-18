import com.gradleup.librarian.gradle.Librarian

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("distribution")
  id("org.jetbrains.kotlin.plugin.compose")
}

Librarian.module(project)

dependencies {
  implementation(project(":librarian-core"))
  api(libs.clikt)
  implementation(libs.mosaic.runtime)

  testImplementation(kotlin("test"))
}

val startScriptTaskProvider =
  tasks.register("createStartScript", org.gradle.jvm.application.tasks.CreateStartScripts::class.java) {
    outputDir = file("build/start_scripts/")
    //mainClass.set("com.gradleup.librarian.repo.Update_repoKt")
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
