package com.gradleup.librarian.gradle

import com.gradleup.librarian.core.tooling.init.SonatypeRelease
import com.gradleup.librarian.core.tooling.init.apiBaseUrl
import com.gradleup.librarian.core.tooling.init.snapshotsUrl
import com.gradleup.librarian.gradle.internal.task.UploadToGcsTask
import com.gradleup.librarian.gradle.internal.task.UploadToNexusTask
import com.gradleup.librarian.gradle.internal.task.UploadToPortalTask
import com.gradleup.librarian.gradle.internal.task.registerGenerateStaticContentTaskTask
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

internal val librarianUploadFilesToSnapshots = "librarianUploadFilesToSnapshots"

internal val librarianDeployToPortal = "librarianDeployToPortal"

internal val librarianPublishToMavenCentral = "librarianPublishToMavenCentral"
internal val librarianPublishToSnapshots = "librarianPublishToSnapshots"
internal val librarianPublishToGcs = "librarianPublishToGcs"

internal val skipProjectIsolationIncompatibleParts = false

@Deprecated("use Librarian.root() instead", ReplaceWith("Librarian.root(project)", "import com.gradleup.librarian.gradle.Librarian"))
fun Project.librarianRoot() {
  if (!skipProjectIsolationIncompatibleParts) {
    configureBcv()
  }

  val properties = project.rootProperties()
  val pomMetadata = PomMetadata(project, properties.kdocArtifactId(), properties)
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

  val allFilesConfiguration = configurations.detachedConfiguration()
  subprojects.forEach {
    allFilesConfiguration.dependencies.add(dependencies.project(it.path, librarianPublication))
  }
  val allFiles = allFilesConfiguration.incoming.artifactView { it.lenient(true) }.files

  registerGenerateStaticContentTaskTask(
    taskName = "librarianStaticContent",
    repositoryFiles = allFiles,
    kdocFiles = fileTree("build/dokka/html"),
    outputDirectory = layout.buildDirectory.dir("static")
  ).configure {
    it.dependsOn("dokkatooGeneratePublicationHtml")
  }

  val mavenCentralTaskProvider = tasks.register(librarianDeployToPortal, UploadToPortalTask::class.java) {
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.files.from(allFiles)
    it.deploymentDescription.set(deploymentDescription)
    it.automatic.set(SonatypeRelease.Automatic == sonatype.release)
    it.version.set(pomMetadata.version)
    it.baseUrl.set(sonatype.baseUrl ?: apiBaseUrl)
  }

  val snapshotsTaskProvider = tasks.register(librarianUploadFilesToSnapshots, UploadToNexusTask::class.java) {
    it.url.set(snapshotsUrl)
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.files.from(allFiles)

    it.enabled = pomMetadata.version.endsWith("-SNAPSHOT")
  }

  tasks.register(librarianPublishToMavenCentral) {
    it.dependsOn(mavenCentralTaskProvider)
  }
  tasks.register(librarianPublishToSnapshots) {
    it.dependsOn(snapshotsTaskProvider)
  }

  val gcs = Gcs(properties)
  tasks.register(librarianPublishToGcs, UploadToGcsTask::class.java) {
    it.files.from(allFiles)
    it.bucket.set(gcs.bucket)
    it.prefix.set(gcs.prefix)
    it.googleServicesJson.set(System.getenv("LIBRARIAN_GOOGLE_SERVICES_JSON"))
  }
}
