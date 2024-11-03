import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm").version("2.0.0").apply(false)
  id("com.gradleup.librarian").version("0.0.7-SNAPSHOT-2707254bba17a35aa7ad75ca9c151f36256f07c0").apply(false)
}

Librarian.root(project)
