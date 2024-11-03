package com.gradleup.librarian.gradle

import org.gradle.api.Project

class Librarian {
  companion object {
    @Suppress("DEPRECATION")
    fun root(project: Project) {
      project.librarianRoot()
    }

    @Suppress("DEPRECATION")
    fun module(project: Project) {
      project.librarianModule()
    }
  }
}