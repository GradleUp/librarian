package com.gradleup.librarian.gradle

import com.gradleup.librarian.gradle.internal.task.registerGenerateStaticContentTaskTask
import com.gradleup.librarian.gradle.internal.task.registerPublishToGcsTask
import nmcp.NmcpAggregationExtension
import nmcp.internal.nmcpConsumerConfigurationName
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

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

  pluginManager.apply("com.gradleup.nmcp.aggregation")

  val nmcpAggregation = extensions.getByType(NmcpAggregationExtension::class.java)
  nmcpAggregation.apply {
    centralPortal {
      it.username.set(sonatype.username)
      it.password.set(sonatype.password)
      it.publishingType.set(sonatype.publishingType)
    }
  }

  val gcs = Gcs(properties)
  if (gcs.bucket != null) {
    Librarian.registerGcsTask(
      this,
      provider { gcs.bucket },
      provider { gcs.prefix },
      provider { System.getenv("LIBRARIAN_GOOGLE_SERVICES_JSON") },
      nmcpAggregation.allFiles
    )
  }

  subprojects.forEach {
    configurations.getByName(nmcpConsumerConfigurationName).dependencies.add(dependencies.create(it))
  }

  registerGenerateStaticContentTaskTask(
    taskName = "librarianStaticContent",
    repositoryFiles = nmcpAggregation.allFiles,
    kdocFiles = fileTree("build/dokka/html"),
    outputDirectory = layout.buildDirectory.dir("static")
  ).configure {
    it.dependsOn("dokkatooGeneratePublicationHtml")
  }

  tasks.register("librarianPublishToSnapshots") {
    it.dependsOn("nmcpPublishAggregationToCentralPortalSnapshots")
  }
  tasks.register("librarianPublishToMavenCentral") {
    it.dependsOn("nmcpPublishAggregationToCentralPortal")
  }
}
