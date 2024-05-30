package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.findGradleProperty
import org.gradle.api.Project

class LibraryModule(
    val javaCompatibility: Int?,
    val kotlinCompatibility: String?,
    val libraryModuleKdoc: LibraryModuleKdoc?,
    val publishing: Publishing?,
    val sympathyForMrMaven: Boolean
) {
  class Builder internal constructor(internal val project: Project) {
    var javaCompatibility: Int? = null
    var kotlinCompatibility: String? = null
    var libraryModuleKdocBuilder: LibraryModuleKdoc.Builder? = null
    var publishingBuilder: Publishing.Builder? = null
    var sympathyForMrMaven: Boolean = true

    fun kdoc(initialize: LibraryModuleKdoc.Builder.() -> Unit) {
      if (libraryModuleKdocBuilder == null) {
        libraryModuleKdocBuilder = LibraryModuleKdoc.Builder(project)
      }
      libraryModuleKdocBuilder?.initialize()
    }

    fun publishing(initialize: Publishing.Builder.() -> Unit) {
      if (publishingBuilder == null) {
        publishingBuilder = Publishing.Builder(project)
      }
      publishingBuilder?.initialize()
    }

    fun fromGradlePropertiesAndEnvironmentVariables() {
      javaCompatibility = project.findGradleProperty("librarian.javaCompatibility")?.toInt()
      kotlinCompatibility = project.findGradleProperty("librarian.kotlinCompatibility")
      sympathyForMrMaven = project.findGradleProperty("librarian.sympathyForMrMaven")?.toBooleanStrict() != false

      if (project.findGradleProperty("librarian.kdoc")?.toBooleanStrictOrNull() != false) {
        kdoc { fromGradleProperties() }
      }

      if (project.findGradleProperty("librarian.publishing")?.toBooleanStrictOrNull() != false) {
        publishing { fromGradlePropertiesAndEnvironmentVariables() }
      }
    }

    fun build(): LibraryModule {
      return LibraryModule(
          javaCompatibility = javaCompatibility,
          kotlinCompatibility = kotlinCompatibility,
          libraryModuleKdoc = libraryModuleKdocBuilder?.build(),
          publishing = publishingBuilder?.build(),
          sympathyForMrMaven = sympathyForMrMaven
      )
    }
  }
}

fun Project.librarianModule(block: (LibraryModule.Builder.() -> Unit) = { fromGradlePropertiesAndEnvironmentVariables() }) {
  val libraryModule = LibraryModule.Builder(this).apply(block).build()

  libraryModule.javaCompatibility?.let {
    configureJavaCompatibility(it)
  }
  libraryModule.kotlinCompatibility?.let {
    configureKotlinCompatibility(it)
  }
  libraryModule.libraryModuleKdoc?.let {
    configureDokkatooModule(it)
  }

  if (libraryModule.sympathyForMrMaven) {
    pluginManager.apply("com.gradleup.maven-sympathy")
  }

  libraryModule.publishing?.let(::configurePublishing)
}
