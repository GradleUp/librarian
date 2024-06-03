package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.configurationLibrarianRepositoryId
import com.gradleup.librarian.core.internal.dependsOn
import com.gradleup.librarian.core.internal.isTag
import com.gradleup.librarian.core.internal.pushedRef
import com.gradleup.librarian.core.internal.registerCreateRepoIdTask
import com.gradleup.librarian.core.internal.registerReleaseTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

fun Project.librarianRoot() {
  configureBcv()

  val properties = project.rootProperties()
  val pomMetadata = PomMetadata(properties.kdocArtifactId(), properties)
  val sonatype = Sonatype(project, properties)
  val signing = Signing(project, properties)

  val kdocWithoutOlder = configureDokkatooAggregate(
      currentVersion = pomMetadata.version,
      olderVersions = properties.olderVersions()
  )
  configurePublishingInternal {
    publications.create("kdoc", MavenPublication::class.java) {
      it.artifact(kdocWithoutOlder)
    }
  }
  configurePom(pomMetadata)

  val createRepoTask = registerCreateRepoIdTask(sonatype, pomMetadata.groupId, pomMetadata.version)

  val configuration = configurations.create(configurationLibrarianRepositoryId) {
    it.isCanBeConsumed = true
    it.isVisible = false
  }
  artifacts.add(configuration.name, createRepoTask)

  configureRepositoriesRoot(sonatype, createRepoTask)
  configureSigning(signing)

  val releaseRepoTask = registerReleaseTask(
      sonatype = sonatype,
      repoId = createRepoTask.map { it.output.get().asFile.readText() },
  )

  val publishToStaging = tasks.register("librarianPublishToStaging")
  val publishToSnapshots = tasks.register("librarianPublishToSnapshots")
  val publishIfNeeded = tasks.register("librarianPublishIfNeeded")

  allprojects { otherProject ->
    otherProject.afterEvaluate {
      it.configureSubproject(publishToStaging, publishToSnapshots, createRepoTask)
    }
  }

  releaseRepoTask.dependsOn(publishToStaging)

  when {
    isTag() -> {
      publishIfNeeded.dependsOn(releaseRepoTask)
    }
    pushedRef() == "refs/heads/${properties.gitSnapshots()}" -> {
      publishIfNeeded.dependsOn(releaseRepoTask)
    }
  }
}

private fun <T1: Task, T2: Task> Project.configureSubproject(
    publishToStaging: TaskProvider<Task>,
    publishToSnapshots: TaskProvider<T1>,
    createRepoTaskProvider: TaskProvider<T2>,
) {
  tasks.all {
    when {
      it.name.endsWith("ToSonatypeStagingRepository") -> {
        it.dependsOn(createRepoTaskProvider)
        publishToStaging.dependsOn(it)
      }
      it.name.endsWith("ToSonatypeSnapshotsRepository") -> {
        publishToSnapshots.dependsOn(it)
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