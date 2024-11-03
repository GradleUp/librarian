package com.gradleup.librarian.gradle.internal.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateStaticContentTask: DefaultTask() {
  @get:InputFiles
  abstract val repositoryFiles: ConfigurableFileCollection

  @get:InputFiles
  abstract val kdocFiles: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun taskAction() {
    outputDirectory.get().asFile.let { base ->
      base.deleteRecursively()
      base.mkdirs()
      repositoryFiles.asFileTree.visit { source ->
        if (source.file.isDirectory) {
          return@visit
        }
        base.resolve("m2").resolve(source.path).let { destination ->
          destination.parentFile.mkdirs()
          source.file.copyTo(destination)
        }
      }
      kdocFiles.asFileTree.visit { source ->
        if (source.file.isDirectory) {
          return@visit
        }
        base.resolve("kdoc").resolve(source.path).let { destination ->
          destination.parentFile.mkdirs()
          source.file.copyTo(destination)
        }
      }
    }
  }
}