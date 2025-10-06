package com.gradleup.librarian.gradle

import com.gradleup.librarian.gradle.internal.task.registerGenerateStaticContentTaskTask
import nmcp.NmcpAggregationExtension
import org.gradle.api.Project

@Deprecated("use Librarian.root() instead", ReplaceWith("Librarian.root(project)", "import com.gradleup.librarian.gradle.Librarian"))
fun Project.librarianRoot() {
  val rootProperties = project.rootProperties()
  val pomMetadata = PomMetadata(rootProperties.kdocArtifactId(), rootProperties)
  val sonatype = Sonatype(project, rootProperties)
  val signing = Signing(project)
  val gcs = Gcs(rootProperties)
  val kdoc = Kdoc(rootProperties)

  librarianRoot(
    group = rootProperties.getRequiredProperty("pom.groupId"),
    version = updateVersionAccordingToEnvironment(rootProperties.getRequiredProperty("pom.version")),
    publishing = Publishing(
      pomMetadata = pomMetadata,
      createMissingPublications = false,
      publishPlatformArtifactsInRootModule = false,
      emptyJarLink = null
    ),
    sonatype = sonatype,
    signing = signing,
    gcs = gcs,
    kdoc = kdoc
  )
}


fun Project.librarianRoot(
  group: String,
  version: String,
  publishing: Publishing,
  signing: Signing,
  sonatype: Sonatype,
  gcs: Gcs?,
  kdoc: Kdoc,
) {
  librarianModule(group, version, null, null, null, null, publishing, signing)

  pluginManager.apply("com.gradleup.nmcp.aggregation")
  pluginManager.apply("com.gradleup.nmcp")

  configureDokkaAggregate(
    currentVersion = version,
    olderVersions = kdoc.olderVersions,
    aggregateConfiguration = "nmcpAggregation",
    includeSelf = kdoc.includeSelf,
  )

  val nmcpAggregation = extensions.getByType(NmcpAggregationExtension::class.java)
  nmcpAggregation.apply {
    centralPortal {
      it.username.set(sonatype.username)
      it.password.set(sonatype.password)
      it.publishingType.set(sonatype.publishingType)
      it.validationTimeout.set(sonatype.validationTimeout)
      it.publishingTimeout.set(sonatype.publishingTimeout)
    }
  }

  if (gcs != null) {
    Librarian.registerGcsTask(
      this,
      provider { gcs.bucket },
      provider { gcs.prefix.trimEnd('/') + '/' },
      provider { gcs.serviceAccountJson },
      nmcpAggregation.allFiles
    )
  }

  registerGenerateStaticContentTaskTask(
    taskName = "librarianStaticContent",
    taskDescription = "Generates `static/m2` and `static/kdoc` folders containing a SNAPSHOT and the KDoc respectively.",
    repositoryFiles = nmcpAggregation.allFiles,
    kdocFiles = fileTree("build/dokka/html"),
    outputDirectory = layout.buildDirectory.dir("static")
  ).configure {
    it.dependsOn("dokkaGeneratePublicationHtml")
  }

  tasks.register("librarianPublishToSnapshots") {
    it.dependsOn("nmcpPublishAggregationToCentralPortalSnapshots")
  }
  tasks.register("librarianPublishToMavenCentral") {
    it.dependsOn("nmcpPublishAggregationToCentralPortal")
  }
}