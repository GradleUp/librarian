package com.gradleup.librarian.gradle

import com.gradleup.librarian.gradle.internal.findEnvironmentVariable
import org.gradle.api.Project
import java.util.Properties

internal fun Properties.javaCompatibility(): Int? {
  return getProperty("java.compatibility")?.toInt()
}

internal fun Properties.kotlinCompatibility(): String? {
  return getProperty("kotlin.compatibility")
}

internal fun Properties.versionPackageName(): String? {
  return getProperty("version.packageName")
}

internal fun Properties.gitSnapshots(): String {
  return getProperty("git.snapshots") ?: "main"
}

internal fun Properties.kdocArtifactId(): String {
  return getProperty("kdoc.artifactId") ?: "kdoc"
}

internal fun Properties.olderVersions(): List<Coordinates> {
  return getProperty("kdoc.olderVersions")?.split(",")
  .orEmpty()
      .filter {
        it.isNotEmpty()
      }
      .map {
        Coordinates(it)
      }
}

fun Project.configureMavenFriendlyDependencies() {
  pluginManager.apply("com.gradleup.maven-sympathy")
}


internal fun Project.rootProperties(): Properties {
  val propertiesFile = rootProject.file("librarian.properties")
  check(propertiesFile.exists()) {
    "No librarian.properties file found at ${propertiesFile.absolutePath}"
  }

  return Properties().apply {
    propertiesFile.inputStream().use {
      load(it)
    }
  }
}

internal fun Project.moduleProperties(): Properties {
  val propertiesFile = file("librarian.properties")
  if(!propertiesFile.exists()) {
    return Properties()
  }

  return Properties().apply {
    propertiesFile.inputStream().use {
      load(it)
    }
  }
}

fun Project.librarianModule() {
  val rootProperties = rootProperties()
  val moduleProperties = moduleProperties()

  rootProperties.javaCompatibility()?.let {
    configureJavaCompatibility(it)
  }

  rootProperties.kotlinCompatibility()?.let {
    configureKotlinCompatibility(it)
  }

  val pomMetadata = PomMetadata(project.name, rootProperties)

  moduleProperties.versionPackageName()?.let {
    configureGeneratedVersion(it, pomMetadata.version)
  }

  configureDokkatoo()

  configurePublishing(
      createMissingPublications = true,
      publishPlatformArtifactsInRootModule = true,
      pomMetadata = pomMetadata,
      sonatype = Sonatype(project, rootProperties),
      signing = Signing(project, rootProperties)
  )
}

internal fun Sonatype(project: Project, properties: Properties): Sonatype {
  val usernameVariable = properties.getProperty("sonatype.username.environmentVariable") ?: "LIBRARIAN_SONATYPE_USERNAME"
  val passwordVariable = properties.getProperty("sonatype.password.environmentVariable") ?: "LIBRARIAN_SONATYPE_PASSWORD"

  return Sonatype(
      username = project.findEnvironmentVariable(usernameVariable),
      password = project.findEnvironmentVariable(passwordVariable),
      host = SonatypeHost.valueOf(properties.getRequiredProperty("sonatype.host"))
  )
}

internal fun Signing(project: Project, properties: Properties): Signing {
  val usernameVariable = properties.getProperty("signing.privateKey.environmentVariable") ?: "LIBRARIAN_SIGNING_PRIVATE_KEY"
  val passwordVariable = properties.getProperty("signing.privateKeyPassword.environmentVariable") ?: "LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD"
  return Signing(
      privateKey = project.findEnvironmentVariable(usernameVariable),
      privateKeyPassword = project.findEnvironmentVariable(passwordVariable)
  )
}

