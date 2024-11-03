package com.gradleup.librarian.gradle

import org.gradle.api.Project

class Librarian {
  companion object {
    fun root(project: Project) {
      project.librarianRoot()
    }

    fun module(project: Project) {
      project.librarianModule()
    }
  }
}