package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.configurationLibrarianRepositoryId
import com.gradleup.librarian.core.internal.dependsOn
import com.gradleup.librarian.core.internal.findGradleProperty
import com.gradleup.librarian.core.internal.isTag
import com.gradleup.librarian.core.internal.pushedRef
import com.gradleup.librarian.core.internal.registerCreateRepoIdTask
import com.gradleup.librarian.core.internal.registerReleaseTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider


class Root(
    val publishing: Publishing,
    val snapshotsBranch: String,
) {
  class Builder internal constructor(internal val project: Project) {
    var snapshotsBranch: String = "main"
    var publishingBuilder: Publishing.Builder = Publishing.Builder(project)

    fun publishing(initializer: Publishing.Builder.() -> Unit) {
      publishingBuilder.initializer()
    }

    fun fromGradlePropertiesAndEnvironmentVariables() {
      project.findGradleProperty("librarian.snapshotsBranch")?.let {
        snapshotsBranch = it
      }
      publishing { fromGradlePropertiesAndEnvironmentVariables() }
    }

    fun build(): Root {
      return Root(
          publishing = publishingBuilder.build(),
          snapshotsBranch = snapshotsBranch
      )
    }
  }
}

fun Project.librarianRoot(block: Root.Builder.() -> Unit = { fromGradlePropertiesAndEnvironmentVariables() }) {
  val root = Root.Builder(this).apply(block).build()

  configureBcv()

  val sonatype = root.publishing.sonatype
  val pomMetadata = root.publishing.pomMetadata
  val createRepoTask = registerCreateRepoIdTask(sonatype, pomMetadata.groupId, pomMetadata.version)

  val configuration = configurations.create(configurationLibrarianRepositoryId) {
    it.isCanBeConsumed = true
    it.isVisible = false
  }
  artifacts.add(configuration.name, createRepoTask)

  val releaseRepoTask = registerReleaseTask(
      sonatype = sonatype,
      repoId = createRepoTask.map { it.output.get().asFile.readText() },
  )

  val ciTaskProvider = tasks.register("ci")

  allprojects { otherProject ->
    otherProject.afterEvaluate {
      it.configureSubproject(ciTaskProvider, createRepoTask, releaseRepoTask, root.snapshotsBranch)
    }
  }
}

private fun <T1: Task, T2: Task> Project.configureSubproject(
    ciTaskProvider: TaskProvider<Task>,
    createRepoTaskProvider: TaskProvider<T1>,
    releaseRepoTaskProvider: TaskProvider<T2>,
    snapshotsBranch: String
) {
  tasks.all {
    when {
      //name == "build" -> ciTaskProvider.dependsOn(it)
      it.name.endsWith("ToSonatypeStagingRepository") -> {
        it.dependsOn(createRepoTaskProvider)
        if (isTag()) {
          ciTaskProvider.dependsOn(it)
        }
      }
      it.name == "publishAllPublicationsToSonatypeSnapshotsRepository" -> {
        if (pushedRef() == "ref/heads/${snapshotsBranch}") {
          ciTaskProvider.dependsOn(it)
        }
      }
    }
  }
}

fun TaskContainer.namedOrNull(name: String): TaskProvider<Task>? {
  return try {
    named(name)
  } catch (e: UnknownTaskException) {
    null
  }
}