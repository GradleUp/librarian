import com.gradleup.librarian.gradle.Librarian

plugins {
  id("base") // Unused
}

buildscript {
  dependencies {
    classpath(libs.kotlin.gradle.plugin)
    classpath(libs.librarian.gradle.plugin)
  }
}

Librarian.root(project)

tasks.register("librarianInstallNpm", Exec::class.java) {
  enabled = file("docs").exists()

  commandLine("npm", "ci")
  workingDir("docs")
}

tasks.register("librarianBuildDocs", Exec::class.java) {
  dependsOn("librarianInstallNpm")
  
  enabled = file("docs").exists()

  commandLine("npm", "run", "build")
  workingDir("docs")
}

tasks.named("librarianStaticContent").configure {
  dependsOn("librarianBuildDocs")

  val from = file("docs/dist")
  doLast {
    from.copyRecursively(outputs.files.single(), overwrite = true)
  }
}