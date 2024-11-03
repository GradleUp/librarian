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
