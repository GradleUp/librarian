package com.gradleup.librarian.gradle

import com.gradleup.librarian.core.tooling.init.snapshotsUrl
import com.gradleup.librarian.gradle.internal.task.registerGenerateStaticContentTaskTask
import com.gradleup.librarian.gradle.internal.task.registerNmcpPublishWithPublisherApiTask
import nmcp.internal.task.registerNmcpPublishFileByFileTask
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

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

  registerNmcpPublishWithPublisherApiTask(
    taskName = librarianPublishToMavenCentral,
    username = provider { sonatype.username },
    password = provider { sonatype.password },
    publicationName = provider { null },
    publishingType = provider { sonatype.publishingType },
    baseUrl = provider { null },
    validationTimeoutSeconds = provider { null },
    publishingTimeoutSeconds = provider { null },
    inputFiles = allFiles
  )

  registerNmcpPublishFileByFileTask(
    taskName = librarianPublishToSnapshots,
    username = provider { sonatype.username },
    password = provider { sonatype.password },
    inputFiles = allFiles,
    url = provider { snapshotsUrl }
  )

  val gcs = Gcs(properties)
  registerNmcpPublishFileByFileTask(
    taskName = librarianPublishToGcs,
    username = provider { "unused" },
    password = provider { System.getenv("LIBRARIAN_GOOGLE_SERVICES_JSON") },
    inputFiles = allFiles,
    url = provider { "gcs://${gcs.bucket}/${gcs.prefix}" }
  )
}
