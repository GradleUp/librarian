package com.gradleup.librarian.gradle.internal.task

import com.gradleup.librarian.gradle.nexusStagingClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import kotlin.time.Duration.Companion.minutes

internal abstract class CloseAndMaybeReleaseRepositoryTask: DefaultTask() {
  @get:Input
  abstract val username: Property<String>

  @get:Input
  abstract val password: Property<String>

  @get:Input
  abstract val baseUrl: Property<String>

  @get:Input
  abstract val repoId: Property<String>

  @get:Input
  abstract val automatic: Property<Boolean>

  @TaskAction
  fun taskAction() {
    runBlocking {
      val nexusStagingClient = nexusStagingClient(baseUrl.get(), username.get(), password.get())
      nexusStagingClient.closeRepositories(listOf(repoId.get()))
      withTimeout(30.minutes) {
        nexusStagingClient.waitForClose(repoId.get(), 1000) {
          logger.log(LogLevel.INFO, ".")
        }
      }
      if (automatic.get()) {
        nexusStagingClient.releaseRepositories(listOf(repoId.get()), true)
      } else {
        logger.log(LogLevel.LIFECYCLE, "Artifacts uploaded, go to ${baseUrl.get()} to release them")
      }
    }
  }
}