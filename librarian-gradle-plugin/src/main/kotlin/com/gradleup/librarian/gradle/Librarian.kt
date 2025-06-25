package com.gradleup.librarian.gradle

import com.gradleup.librarian.gradle.internal.task.registerPublishToGcsTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider

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
    fun registerGcsTask(
        project: Project,
        bucket: Provider<String>,
        prefix: Provider<String>,
        credentials: Provider<String>,
        files: FileCollection
    ) {
      project.registerPublishToGcsTask(
        taskName = "librarianPublishToGcs",
        bucket = bucket,
        prefix = prefix,
        credentials = credentials,
        inputFiles = files
      )
    }
  }
}