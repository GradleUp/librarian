import com.gradleup.librarian.gradle.librarianRoot

plugins {
  id("{{kotlinPluginId}}").version("2.0.0").apply(false)
  id("com.gradleup.librarian").version("{{librarianVersion}}").apply(false)
}

librarianRoot()