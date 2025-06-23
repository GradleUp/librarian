package com.gradleup.librarian.gradle.internal.task

import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GManuallyWired
import gratatouille.tasks.GOutputDirectory
import gratatouille.tasks.GTask

@GTask
fun generateStaticContentTask(
  repositoryFiles: GInputFiles,
  kdocFiles: GInputFiles,
  @GManuallyWired
  outputDirectory: GOutputDirectory
) {
  outputDirectory.let { base ->
    base.deleteRecursively()
    base.mkdirs()
    repositoryFiles.forEach { source ->
      if (source.file.isDirectory) {
        return@forEach
      }
      base.resolve("m2").resolve(source.normalizedPath).let { destination ->
        destination.parentFile.mkdirs()
        source.file.copyTo(destination)
      }
    }
    kdocFiles.forEach { source ->
      if (source.file.isDirectory) {
        return@forEach
      }
      base.resolve("kdoc").resolve(source.normalizedPath).let { destination ->
        destination.parentFile.mkdirs()
        source.file.copyTo(destination)
      }
    }
  }
}