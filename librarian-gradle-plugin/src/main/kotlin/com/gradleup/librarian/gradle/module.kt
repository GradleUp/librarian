package com.gradleup.librarian.gradle

import com.gradleup.librarian.core.tooling.init.modulePropertiesFilename
import com.gradleup.librarian.core.tooling.init.rootPropertiesFilename
import com.gradleup.librarian.gradle.internal.findEnvironmentVariable
import tapmoc.configureJavaCompatibility
import tapmoc.configureKotlinCompatibility
import org.gradle.api.Project
import java.time.Duration
import java.util.Properties

internal fun Properties.javaCompatibility(): Int {
  return getRequiredProperty("java.compatibility").toInt()
}

internal fun Properties.kotlinCompatibility(): String {
  return getRequiredProperty("kotlin.compatibility")
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

@Deprecated(
  "use Librarian.module() instead.",
  ReplaceWith("Librarian.module(project)", "import com.gradleup.librarian.gradle.Librarian")
)
fun Project.librarianModule() {
  val rootProperties = rootProperties()
  val moduleProperties = moduleProperties()

  val bcv = if (moduleProperties.getProperty("bcv") != "false") {
    Bcv(rootProperties.get("bcv.warn") != "false") {}
  } else {
    null
  }

  val publish = moduleProperties.publish() ?: true

  librarianModule(
    group = rootProperties.getRequiredProperty("pom.groupId"),
    version = updateVersionAccordingToEnvironment(rootProperties.getRequiredProperty("pom.version")),
    jvmTarget = rootProperties.javaCompatibility(),
    kotlinTarget = rootProperties.kotlinCompatibility(),
    bcv = bcv,
    versionPackageName = moduleProperties.versionPackageName(),
    publishing = Publishing(
      createMissingPublications = moduleProperties.createMissingPublications() ?: true,
      publishPlatformArtifactsInRootModule = true,
      pomMetadata = PomMetadata(null, rootProperties),
      emptyJarLink = "https://www.apollographql.com/docs/kotlin/kdoc/index.html"
    ).takeIf{publish},
    signing = Signing(project),
  )
}

class Bcv(
  val warnIfMissing: Boolean,
  val variantSpec: (variantSpec: Any) -> Unit
)

class Publishing(
  val createMissingPublications: Boolean,
  val publishPlatformArtifactsInRootModule: Boolean,
  val pomMetadata: PomMetadata,
  val emptyJarLink: String?
)

fun Project.librarianModule(
  group: String,
  version: String,
  jvmTarget: Int?,
  kotlinTarget: String?,
  bcv: Bcv?,
  versionPackageName: String?,
  publishing: Publishing?,
  signing: Signing?,
) {
  /**
   * Set `project.group` and version. This is needed when including builds
   */
  this.group = group
  this.version = version

  jvmTarget?.let(::configureJavaCompatibility)
  kotlinTarget?.let(::configureKotlinCompatibility)

  if (versionPackageName != null) {
    configureGeneratedVersion(versionPackageName, version)
  }

  if (publishing != null) {
    configurePublishing(
      group = group,
      version = version,
      createMissingPublications = publishing.createMissingPublications,
      publishPlatformArtifactsInRootModule = publishing.publishPlatformArtifactsInRootModule,
      pomMetadata = publishing.pomMetadata,
      signing = signing,
      emptyJarLink = publishing.emptyJarLink
    )
    /**
     * Only configure BCV for projects that are published.
     * This is because we don't need to track projects that do not get published.
     */
    if (bcv != null) {
      configureBcv(bcv.warnIfMissing, bcv.variantSpec)
    }
    /**
     * Only configure signing for projects that are published.
     * This is because signing needs the 'publishing' plugin applied.
     */
    if (signing != null) {
      configureSigning(signing)
    }
    /**
     * Only configure Dokka for projects that are published.
     * This is because we don't want those in the aggregate KDoc output.
     */
    configureDokka()
  }
}

internal fun Sonatype(project: Project, properties: Properties): Sonatype {
  check(properties.getProperty("sonatype.release") == null) {
    "Librarian: sonatype.release is not used anymore, use sonatype.publishingType instead."
  }
  check(properties.getProperty("sonatype.backend") == null) {
    "Librarian: sonatype.backend is not used anymore, sonatype publishing is always done on the central portal."
  }
  check(properties.getProperty("sonatype.baseUrl") == null) {
    "Librarian: sonatype.baseUrl is not used anymore."
  }
  return Sonatype(
    username = project.findEnvironmentVariable("LIBRARIAN_SONATYPE_USERNAME"),
    password = project.findEnvironmentVariable("LIBRARIAN_SONATYPE_PASSWORD"),
    publishingType = properties.getProperty("sonatype.publishingType"),
    validationTimeout = properties.getProperty("sonatype.validationTimeout")?.let(Duration::parse),
    publishingTimeout = properties.getProperty("sonatype.publishingTimeout")?.let(Duration::parse),
  )
}

internal fun Signing(project: Project): Signing {
  return Signing(
    privateKey = project.findEnvironmentVariable("LIBRARIAN_SIGNING_PRIVATE_KEY"),
    privateKeyPassword = project.findEnvironmentVariable("LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD")
  )
}
