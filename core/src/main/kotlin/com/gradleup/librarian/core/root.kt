package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.isTag
import com.gradleup.librarian.core.internal.pushedRef
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

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

  val configuration = configurations.create(CONFIGURATION_REPO_ID) {
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
  val publishToSnapshots = tasks.register("librarianPublishToSnapshots") {
    it.doLast {
      it.inputs.files.forEach {
        println("${it.name}: ${it.readText()}")
      }
    }
  }
  val publishIfNeeded = tasks.register("librarianPublishIfNeeded")


  val releaseConfiguration = configurations.detachedConfiguration()
  val snapshotConfiguration = configurations.detachedConfiguration()

  subprojects {
    releaseConfiguration.dependencies.add(dependencies.project(it.path, CONFIGURATION_RELEASE_DATE))
    snapshotConfiguration.dependencies.add(dependencies.project(it.path, CONFIGURATION_SNAPSHOT_DATE))
  }

  publishToStaging.configure {
    it.inputs.files(releaseConfiguration.incoming.artifactView { it.lenient(true) }.files)
  }
  publishToSnapshots.configure {
    it.inputs.files(snapshotConfiguration)
    //it.inputs.files(snapshotConfiguration.incoming.artifactView { it.lenient(true) }.files)
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
