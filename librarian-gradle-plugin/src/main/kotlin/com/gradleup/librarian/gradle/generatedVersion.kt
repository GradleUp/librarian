package com.gradleup.librarian.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File

internal const val LIBRARIAN_GENERATE_VERSION = "librarianGenerateVersion"

fun Project.configureGeneratedVersion(packageName: String, pomVersion: String) {
  val kotlin = kotlinExtensionOrNull
  if (kotlin == null) {
    //TODO: Java
    return
  }

  val pluginVersionTaskProvider = tasks.register(LIBRARIAN_GENERATE_VERSION, GeneratePluginVersion::class.java) {
    it.outputDir.set(project.layout.buildDirectory.dir("task/librarianGenerateVersion"))
    it.version.set(pomVersion)
    it.packageName.set(packageName)
  }

  val sourceSet = kotlin.sourceSets.findByName("main") ?: kotlin.sourceSets.findByName("commonMain")
  check(sourceSet != null) {
    "No 'main' or 'commonMain' sourceSet found"
  }
  val versionFileProvider = pluginVersionTaskProvider.flatMap { it.outputDir }
  sourceSet.kotlin.srcDir(versionFileProvider)

  tasks.withType(KotlinCompilationTask::class.java) {
    it.dependsOn(pluginVersionTaskProvider)
  }
}

abstract class GeneratePluginVersion : DefaultTask() {
  @get:Input
  abstract val version: Property<String>

  @get:Input
  abstract val packageName: Property<String>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun taskAction() {
    val versionFile = File(outputDir.asFile.get(), "version.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package ${packageName.get()}
import kotlin.jvm.JvmField
@JvmField
val VERSION = "${version.get()}"
"""
    )
  }
}


