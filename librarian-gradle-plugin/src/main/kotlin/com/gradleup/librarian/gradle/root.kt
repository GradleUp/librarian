package com.gradleup.librarian.gradle

import com.gradleup.librarian.core.tooling.init.SonatypeBackend
import com.gradleup.librarian.core.tooling.init.SonatypeRelease
import com.gradleup.librarian.gradle.internal.task.*
import com.gradleup.librarian.gradle.internal.task.snapshotsUrl
import com.gradleup.librarian.gradle.internal.task.stagingRepositoryUrl
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider

internal val librarianCreateStagingRepo = "librarianCreateStagingRepo"
internal val librarianUploadFilesToStaging = "librarianUploadFilesToStaging"
internal val librarianCloseAndMaybeReleaseRepository = "librarianCloseAndMaybeReleaseRepository"

internal val librarianUploadFilesToSnapshots = "librarianUploadFilesToSnapshots"

internal val librarianDeployToPortal = "librarianDeployToPortal"

internal val librarianPublishToMavenCentral = "librarianPublishToMavenCentral"
internal val librarianPublishToSnapshots = "librarianPublishToSnapshots"

internal val skipProjectIsolationIncompatibleParts = false

fun Project.librarianRoot() {
  if (!skipProjectIsolationIncompatibleParts) {
    configureBcv()
  }

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
  configureSigning(signing)

  val deploymentDescription = "${pomMetadata.groupId}:${project.name}:${pomMetadata.version}"
  val createRepoTask = tasks.register(librarianCreateStagingRepo, CreateRepositoryTask::class.java) {
    it.output.set(layout.buildDirectory.file("librarian/repositoryId"))
    it.repoDescription.set(deploymentDescription)
    it.baseUrl.set(stagingBaseUrl(sonatype.backend, sonatype.baseUrl))
    it.groupId.set(pomMetadata.groupId)
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.version.set(pomMetadata.version)
  }

  val allFilesConfiguration = configurations.detachedConfiguration()
  subprojects.forEach {
    allFilesConfiguration.dependencies.add(dependencies.project(it.path, librarianPublication))
  }
  val allFiles = allFilesConfiguration.incoming.artifactView { it.lenient(true) }.files

  val librarianStaticContent = tasks.register("librarianStaticContent", GenerateStaticContentTask::class.java) {
    it.dependsOn("dokkatooGeneratePublicationHtml")

    it.repositoryFiles.from(allFiles)
    it.kdocFiles.from(file("build/dokka/html"))

    it.outputDirectory.set(layout.buildDirectory.dir("static"))
  }
  val repoId = createRepoTask.map { it.output.get().asFile.readText() }

  val mavenCentralTaskProvider: TaskProvider<*>
  val snapshotsTaskProvider: TaskProvider<*>?

  if (sonatype.backend == SonatypeBackend.Portal) {
    mavenCentralTaskProvider = tasks.register(librarianDeployToPortal, UploadToPortalTask::class.java) {
      it.username.set(sonatype.username)
      it.password.set(sonatype.password)
      it.files.from(allFiles)
      it.deploymentDescription.set(deploymentDescription)
      it.automatic.set(SonatypeRelease.Automatic == sonatype.release)
      it.version.set(pomMetadata.version)
      it.baseUrl.set(deployBaseUrl(sonatype.baseUrl))
    }
    snapshotsTaskProvider = null
  } else {
    val uploadToStaging = tasks.register(librarianUploadFilesToStaging, UploadToNexusTask::class.java) {
      it.url.set(repoId.map { stagingRepositoryUrl(sonatype.backend, sonatype.baseUrl, it) })
      it.username.set(sonatype.username)
      it.password.set(sonatype.password)
      it.files.from(allFiles)
    }

    mavenCentralTaskProvider = tasks.register(librarianCloseAndMaybeReleaseRepository, CloseAndMaybeReleaseRepositoryTask::class.java) {
      it.baseUrl.set(stagingBaseUrl(sonatype.backend, sonatype.baseUrl))
      it.username.set(sonatype.username)
      it.password.set(sonatype.password)
      it.repoId.set(repoId)
      it.automatic.set(sonatype.release == SonatypeRelease.Automatic)
      it.dependsOn(uploadToStaging)
    }

    snapshotsTaskProvider = tasks.register(librarianUploadFilesToSnapshots, UploadToNexusTask::class.java) {
      it.url.set(snapshotsUrl(sonatype.backend, sonatype.baseUrl))
      it.username.set(sonatype.username)
      it.password.set(sonatype.password)
      it.files.from(allFiles)

      it.enabled = pomMetadata.version.endsWith("-SNAPSHOT")
    }
  }

  tasks.register(librarianPublishToMavenCentral) {
    it.dependsOn(mavenCentralTaskProvider)
  }
  tasks.register(librarianPublishToSnapshots) {
    if (snapshotsTaskProvider != null) {
      it.dependsOn(snapshotsTaskProvider)
    } else {
      it.doLast {
        error("The central portal doesn't support SNAPSHOTs")
      }
    }
  }
}
