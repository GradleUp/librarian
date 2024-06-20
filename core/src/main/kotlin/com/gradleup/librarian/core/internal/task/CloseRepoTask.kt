package com.gradleup.librarian.core.internal.task

import com.gradleup.librarian.core.SonatypeHost
import com.gradleup.librarian.core.nexusStagingClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import kotlin.time.Duration.Companion.minutes

abstract class CloseRepoTask: DefaultTask() {
  @get:Input
  abstract val username: Property<String>

  @get:Input
  abstract val password: Property<String>

  @get:Input
  abstract val host: Property<SonatypeHost>

  @get:Input
  abstract val repoId: Property<String>

  @TaskAction
  fun taskAction() {
    runBlocking {
      val nexusStagingClient = nexusStagingClient(host.get(), username.get(), password.get())
      nexusStagingClient.closeRepositories(listOf(repoId.get()))
      withTimeout(30.minutes) {
        nexusStagingClient.waitForClose(repoId.get(), 1000) {
          logger.log(LogLevel.INFO, ".")
        }
      }
    }
  }
}