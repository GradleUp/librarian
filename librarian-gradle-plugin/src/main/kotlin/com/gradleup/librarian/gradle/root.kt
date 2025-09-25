package com.gradleup.librarian.gradle

import com.gradleup.librarian.gradle.internal.task.registerGenerateStaticContentTaskTask
import nmcp.NmcpAggregationExtension
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

@Deprecated("use Librarian.root() instead", ReplaceWith("Librarian.root(project)", "import com.gradleup.librarian.gradle.Librarian"))
fun Project.librarianRoot() {
  val properties = project.rootProperties()
  val pomMetadata = PomMetadata(properties.kdocArtifactId(), properties)
  val sonatype = Sonatype(project, properties)
  val signing = Signing(project, properties)

  val kdocWithoutOlder = configureDokkaAggregate(
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

  // Apply nmcp here as well for the kdocs
  pluginManager.apply("com.gradleup.nmcp")
  
  /**
   * This doesn't use `nmcpAggregation.publishAllProjectsProbablyBreakingProjectIsolation()`
   * because the individual projects have the plugin applied already so we can do this in
   * a less breaking way (TBC whether this is still breaking or not. I think not but nt 100% sure)
   */
  allprojects.forEach {
    configurations.getByName("nmcpAggregation").dependencies.add(dependencies.create(it))
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


  registerGenerateStaticContentTaskTask(
    taskName = "librarianStaticContent",
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
