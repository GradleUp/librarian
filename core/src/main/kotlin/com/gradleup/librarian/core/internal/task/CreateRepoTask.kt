package com.gradleup.librarian.core.internal.task

import com.gradleup.librarian.core.SonatypeHost
import com.gradleup.librarian.core.nexusStagingClient
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class CreateRepoTask: DefaultTask() {
  @get:Input
  abstract val username: Property<String>

  @get:Input
  abstract val password: Property<String>

  @get:Input
  abstract val sonatypeHost: Property<SonatypeHost>

  @get:Input
  abstract val groupId: Property<String>

  @get:Input
  abstract val repoDescription: Property<String>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val repoId = runBlocking {
      val nexusStagingClient = nexusStagingClient(sonatypeHost.get(), username.get(), password.get())
      val candidates = nexusStagingClient.getProfiles()
      val profileId = when {
        candidates.isEmpty() -> error("No staging profile found")
        candidates.size > 1 -> {
          val found = candidates.singleOrNull { groupId.get().startsWith(it.name) }
          if (found == null) {
            error("Cannot choose between staging profiles: ${candidates.map { "${it.name}(${it.id}" }.joinToString(", ")}")
          }
          found.id
        }
        else -> candidates.single().id
      }
      nexusStagingClient.createRepository(
          profileId = profileId,
          description = repoDescription.get()
      )
    }
    //logger.log(LogLevel.LIFECYCLE, "repo created: $repoId")
    output.asFile.get().writeText(repoId)
  }
}