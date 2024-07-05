package com.gradleup.librarian.gradle.internal.task

import com.gradleup.librarian.gradle.SonatypeHost
import com.gradleup.librarian.gradle.nexusStagingClient
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ReleaseRepoTask: DefaultTask() {
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
      nexusStagingClient.releaseRepositories(listOf(repoId.get()), true)
    }
  }
}