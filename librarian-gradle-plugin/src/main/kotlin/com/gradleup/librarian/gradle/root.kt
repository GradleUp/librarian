package com.gradleup.librarian.gradle

import com.gradleup.librarian.gradle.internal.task.*
import com.gradleup.librarian.gradle.internal.task.snapshotsUrl
import com.gradleup.librarian.gradle.internal.task.stagingRepositoryUrl
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
  configureSigning(signing)

  val deploymentDescription = "${pomMetadata.groupId}:${project.name}:${pomMetadata.version}"
  val createRepoTask = tasks.register("librarianCreateStagingRepo", CreateRepositoryTask::class.java) {
    it.output.set(layout.buildDirectory.file("librarian/repositoryId"))
    it.repoDescription.set(deploymentDescription)
    it.baseUrl.set(stagingBaseUrl(sonatype.backend, sonatype.baseUrl))
    it.groupId.set(pomMetadata.groupId)
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.version.set(pomMetadata.version)
  }

  val allFilesConfiguration = configurations.detachedConfiguration()
  subprojects {
    allFilesConfiguration.dependencies.add(dependencies.project(it.path, librarianPublication))
  }
  val allFiles = allFilesConfiguration.incoming.artifactView { it.lenient(true) }.files

  val repoId = createRepoTask.map { it.output.get().asFile.readText() }

  val uploadToStaging = tasks.register("librarianUploadFilesToStaging", UploadFilesTask::class.java) {
    it.url.set(repoId.map { stagingRepositoryUrl(sonatype.backend, sonatype.baseUrl, it) })
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.files.from(allFiles)
  }

  tasks.register("librarianReleaseRepository", ReleaseRepositoryTask::class.java) {
    it.baseUrl.set(stagingBaseUrl(sonatype.backend, sonatype.baseUrl))
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.repoId.set(repoId)
    it.automatic.set(sonatype.release == SonatypeRelease.Automatic)
    it.dependsOn(uploadToStaging)
  }

  val uploadToSnapshots = tasks.register("librarianUploadFilesToSnapshots", UploadFilesTask::class.java) {
    it.url.set(snapshotsUrl(sonatype.backend, sonatype.baseUrl))
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.files.from(allFiles)

    it.enabled = pomMetadata.version.endsWith("-SNAPSHOT")
  }

  val deployToPortal = tasks.register("librarianDeployToPortal", DeployToPortalTask::class.java) {
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.files.from(allFiles)
    it.deploymentDescription.set(deploymentDescription)
    it.automatic.set(SonatypeRelease.Automatic == sonatype.release)
    it.version.set(pomMetadata.version)
    it.baseUrl.set(deployBaseUrl(sonatype.baseUrl))
  }

  tasks.register("librarianPublishToMavenCentral") {
    if (sonatype.backend == SonatypeBackend.Portal) {
      it.dependsOn("librarianDeployToPortal")
    } else {
      it.dependsOn("librarianReleaseRepository")
    }
  }
  tasks.register("librarianPublishToMavenSnapshots") {
    it.dependsOn("librarianUploadFilesToSnapshots")
  }
}
