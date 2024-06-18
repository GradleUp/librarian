import com.gradleup.librarian.core.librarianModule

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("distribution")
}

librarianModule()

dependencies {
  implementation(project(":core"))
  implementation(libs.clikt)
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
