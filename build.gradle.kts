import com.gradleup.librarian.gradle.Librarian

plugins {
  id("base") // Unused
}

buildscript {
  dependencies {
    classpath(libs.kotlin.gradle.plugin)
    classpath(libs.librarian.gradle.plugin)
    classpath(libs.gratatouille.gradle.plugin)
    classpath(libs.ksp.gradle.plugin)
  }
}

Librarian.root(project)

tasks.register("docsNpmInstall", Exec::class.java) {
  enabled = file("docs").exists()

  commandLine("npm", "ci")
  workingDir("docs")
}

tasks.register("docsNpmBuild", Exec::class.java) {
  dependsOn("docsNpmInstall")

  enabled = file("docs").exists()

  commandLine("npm", "run", "build")
  workingDir("docs")
}

tasks.named("librarianStaticContent").configure {
  dependsOn("docsNpmBuild")

  val from = file("docs/dist")
  doLast {
    from.copyRecursively(outputs.files.single(), overwrite = true)
  }
}