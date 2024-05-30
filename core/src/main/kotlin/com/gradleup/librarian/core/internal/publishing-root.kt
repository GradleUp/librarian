package com.gradleup.librarian.core.internal

import com.gradleup.librarian.core.Publishing
import com.gradleup.librarian.core.Sonatype
import com.gradleup.librarian.core.SonatypeHost
import com.gradleup.librarian.core.internal.task.CreateRepoTask
import com.gradleup.librarian.core.internal.task.ReleaseRepoTask
import com.gradleup.librarian.core.toBaseUrl
import net.mbonnin.vespene.lib.NexusStagingClient
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

internal val configurationLibrarianRepositoryId = "librarianRepositoryId"


internal fun Project.registerCreateRepoIdTask(
    sonatype: Sonatype,
    group: String,
    version: String,
): TaskProvider<CreateRepoTask> {
  return rootProject.tasks.register("librarianCreateStagingRepo", CreateRepoTask::class.java) {
    it.output.set(rootProject.layout.buildDirectory.file("librarianRepoId"))
    it.repoDescription.set("$group:${project.name}:$version")
    it.sonatypeHost.set(sonatype.host)
    it.groupId.set(group)
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    if (sonatype.stagingProfile != null) {
      it.stagingProfile.set(sonatype.stagingProfile)
    }
  }
}

internal fun nexusStagingClient(host: SonatypeHost, username: String, password: String): NexusStagingClient {
  return NexusStagingClient(
      baseUrl = "${host.toBaseUrl()}/service/local/",
      username = username,
      password = password
  )
}

internal fun Project.registerReleaseTask(
    sonatype: Sonatype,
    repoId: Provider<String>,
): TaskProvider<out Task> {
  return tasks.register("librarianReleaseStagingRepo", ReleaseRepoTask::class.java) {
    it.host.set(sonatype.host)
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.repoId.set(repoId)
  }
}


fun <T : Task> TaskProvider<T>.dependsOn(other: Any) {
  configure {
    it.dependsOn(other)
  }
}
