package com.gradleup.librarian.gradle

import com.gradleup.librarian.core.tooling.init.modulePropertiesFilename
import com.gradleup.librarian.core.tooling.init.rootPropertiesFilename
import com.gradleup.librarian.gradle.internal.findEnvironmentVariable
import compat.patrouille.configureJavaCompatibility
import compat.patrouille.configureKotlinCompatibility
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
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

internal fun Properties.publish(): Boolean? {
  return getProperty("publish")?.toBoolean()
}

internal fun Properties.createMissingPublications(): Boolean? {
  return getProperty("createMissingPublications")?.toBoolean()
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

internal fun Project.configureMavenFriendlyDependencies() {
  pluginManager.apply("com.gradleup.maven-sympathy")
}

internal fun Project.rootProperties(): Properties {
  val propertiesFile = rootDir.resolve(rootPropertiesFilename)
  check(propertiesFile.exists()) {
    "No $rootPropertiesFilename file found at ${propertiesFile.absolutePath}"
  }

  return Properties().apply {
    propertiesFile.inputStream().use {
      load(it)
    }
  }
}

internal fun Project.moduleProperties(): Properties {
  val propertiesFile = file(modulePropertiesFilename)
  if (!propertiesFile.exists()) {
    return Properties()
  }

  return Properties().apply {
    propertiesFile.inputStream().use {
      load(it)
    }
  }
}

@Deprecated("use Librarian.module() instead.", ReplaceWith("Librarian.module(project)", "import com.gradleup.librarian.gradle.Librarian"))
fun Project.librarianModule() {
  val rootProperties = rootProperties()
  val moduleProperties = moduleProperties()

  configureBcv(rootProperties)

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
  val publish = moduleProperties.publish() ?: true
  if (publish) {
    configureDokkatoo()

    configurePublishing(
      createMissingPublications = moduleProperties.createMissingPublications() ?: true,
      publishPlatformArtifactsInRootModule = true,
      pomMetadata = pomMetadata,
      signing = Signing(project, rootProperties),
    )
  }
}

internal fun Sonatype(project: Project, properties: Properties): Sonatype {
  val usernameVariable =
    properties.getProperty("sonatype.username.environmentVariable") ?: "LIBRARIAN_SONATYPE_USERNAME"
  val passwordVariable =
    properties.getProperty("sonatype.password.environmentVariable") ?: "LIBRARIAN_SONATYPE_PASSWORD"

  check(properties.getProperty("sonatype.release") == null) {
    "Librarian: sonatype.release is not used anymore, use sonatype.publishingType instead."
  }
  check(properties.getProperty("sonatype.backend") == null) {
    "Librarian: sonatype.backend is not used anymore, sonatype publishing is always done on the central portal."
  }
  return Sonatype(
    username = project.findEnvironmentVariable(usernameVariable),
    password = project.findEnvironmentVariable(passwordVariable),
    publishingType = properties.getProperty("sonatype.publishingType"),
    baseUrl = properties.getProperty("sonatype.baseUrl")?.takeIf { it.isNotBlank() }
  )
}

internal fun Signing(project: Project, properties: Properties): Signing {
  val usernameVariable =
    properties.getProperty("signing.privateKey.environmentVariable") ?: "LIBRARIAN_SIGNING_PRIVATE_KEY"
  val passwordVariable =
    properties.getProperty("signing.privateKeyPassword.environmentVariable") ?: "LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD"
  return Signing(
    privateKey = project.findEnvironmentVariable(usernameVariable),
    privateKeyPassword = project.findEnvironmentVariable(passwordVariable)
  )
}

