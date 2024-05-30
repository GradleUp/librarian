package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.findGradleProperty
import org.gradle.api.Project

class KdocModule(
    val kdocAggregate: KdocAggregate?,
    val publishing: Publishing?,
) {
  class Builder internal constructor(internal val project: Project) {
    var kdocAggregateBuilder: KdocAggregate.Builder? = null
    var publishingBuilder: Publishing.Builder? = null

    fun kdoc(initialize: KdocAggregate.Builder.() -> Unit) {
      if (kdocAggregateBuilder == null) {
        kdocAggregateBuilder = KdocAggregate.Builder(project)
      }
      kdocAggregateBuilder?.initialize()
    }

    fun publishing(initialize: Publishing.Builder.() -> Unit) {
      if (publishingBuilder == null) {
        publishingBuilder = Publishing.Builder(project)
      }
      publishingBuilder?.initialize()
    }

    fun fromGradlePropertiesAndEnvironmentVariables() {
      if (project.findGradleProperty("librarian.kdoc")?.toBooleanStrictOrNull() != false) {
        kdoc { fromGradleProperties() }
      }

      if (project.findGradleProperty("librarian.publishing")?.toBooleanStrictOrNull() != false) {
        publishing { fromGradlePropertiesAndEnvironmentVariables() }
      }
    }

    fun build(): KdocModule {
      return KdocModule(
          kdocAggregate = kdocAggregateBuilder?.build(),
          publishing = publishingBuilder?.build()
      )
    }
  }
}

fun Project.librarianKdoc(block: (KdocModule.Builder.() -> Unit) = { fromGradlePropertiesAndEnvironmentVariables() }) {
  val kdocModule = KdocModule.Builder(this).apply(block).build()

  kdocModule.kdocAggregate?.let {
    configureDokkatooAggregate(
        currentVersion = it.currentVersion,
        olderVersions = it.olderVersions
    )
  }

  kdocModule.publishing?.let { publishing ->
    if (publishing.createMissingPublications) {
      createMissingPublications(publishing.pomMetadata.vcsUrl)
    }
    configurePom(publishing.pomMetadata)
    publishing.sonatype?.let {
      configureRepositories(it)
    }
    publishing.signing?.let {
      configureSigning(it)
    }
  }
}
