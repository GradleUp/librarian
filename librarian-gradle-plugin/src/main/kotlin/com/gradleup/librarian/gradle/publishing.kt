package com.gradleup.librarian.gradle

import com.gradleup.librarian.gradle.internal.createAndroidPublication
import com.gradleup.librarian.gradle.internal.hasAndroid
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


internal fun Project.configurePublishingInternal(block: PublishingExtension.() -> Unit) {
  plugins.apply("maven-publish")
  block(extensions.getByType(PublishingExtension::class.java))
}


class Sonatype(
  val username: String?,
  val password: String?,
  val publishingType: String?,
  val baseUrl: String?
)

class Gcp(
  val bucket: String?,
  val prefix: String // The prefix, always ending with '/'
)

fun Gcs(properties: Properties): Gcp {
  return Gcp(
    properties.getProperty("gcs.bucket"),
    properties.getProperty("gcs.prefix").orEmpty().trimEnd('/') + '/',
  )
}

fun PomMetadata(artifactId: String, properties: Properties): PomMetadata {
  val version = updateVersionAccordingToEnvironment(properties.getRequiredProperty("pom.version"))

  check(properties.getProperty("pom.licenseUrl") == null) {
    "licenseUrl is not used anymore"
  }
  return PomMetadata(
          groupId = properties.getRequiredProperty("pom.groupId"),
          artifactId = artifactId,
          version = version,
          description = properties.getRequiredProperty("pom.description"),
          vcsUrl = properties.getRequiredProperty("pom.vcsUrl"),
          developer = properties.getRequiredProperty("pom.developer"),
          license = properties.getRequiredProperty("pom.license"),
  )
}

internal fun Properties.getRequiredProperty(name: String): String {
  return getProperty(name) ?: error("Librarian: '$name' is required.")
}

/**
 * User metadata for .pom or .module metadata associated with the repository.
 * This metadata is the same in all modules
 */
class PomMetadata(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val description: String,
    val vcsUrl: String,
    val developer: String,
    val license: String,
)

fun Project.configurePublishing(
  createMissingPublications: Boolean,
  publishPlatformArtifactsInRootModule: Boolean,
  pomMetadata: PomMetadata,
  signing: Signing,
) {
  if (createMissingPublications) {
    createMissingPublications(pomMetadata.vcsUrl)
  }
  configurePom(pomMetadata)
  if (publishPlatformArtifactsInRootModule) {
    publishPlatformArtifactsInRootModule()
  }
  configureSigning(signing)

  pluginManager.apply("com.gradleup.nmcp")
}

private fun Project.emptyJavadoc(repositoryUrl: String?): TaskProvider<Jar> {
  return tasks.register("librarianEmptyJavadoc", Jar::class.java) {
    it.archiveClassifier.set("javadoc")
    val extra = repositoryUrl?.let { " or $repositoryUrl" }
    it.from(
        resources.text.fromString(
            """
                This Javadoc JAR is intentionally empty.
                  
                For documentation, see the sources jar$extra
                  
                """.trimIndent()
        )
    ) {
      it.rename { "readme.txt" }
    }
  }
}

private fun Project.javaSources(): TaskProvider<Jar> {
  return tasks.register("librarianSources", org.gradle.jvm.tasks.Jar::class.java) {
    it.archiveClassifier.set("sources")

    val sourceSets = project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
    it.from(sourceSets.getByName("main").allSource)
  }
}

/**
 * Creates missing publications based on the plugins being applied and the tasks found in the project.
 *
 * Some plugins (like the Kotlin multiplatform plugin) create publications automatically but others do not (like the Kotlin JVM one).
 *
 * [createMissingPublications] makes sure a publication is created for this module and that this publication contains source and javadoc jars.
 *
 * The added javadoc jar is empty and only required to pass the Maven Central checks.
 *
 * @param docUrl an optional link to write inside the empty javadoc jar for users looking for more information.
 */
fun Project.createMissingPublications(
    docUrl: String? = null,
) = configurePublishingInternal {
  val emptyJavadoc = emptyJavadoc(docUrl)

  when {
    project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
      /**
       * Kotlin MPP creates publications but doesn't add javadoc.
       * Note: for Android, the caller needs to opt-in puoblication
       * See https://kotlinlang.org/docs/multiplatform-publish-lib.html#publish-an-android-library
       */
      publications.withType(MavenPublication::class.java).configureEach {
        it.artifact(emptyJavadoc)
      }
    }

    project.plugins.hasPlugin("com.gradle.plugin-publish") -> {
      /**
       * com.gradle.plugin-publish creates all publications
       */
    }

    plugins.hasPlugin("java-gradle-plugin") -> {
      /**
       * java-gradle-plugin creates 2 publications (one marker and one regular) but without source/javadoc.
       */
      publications.withType(MavenPublication::class.java) {
        // Only add sources and javadoc for the main publication
        if (!it.name.endsWith("PluginMarkerMaven")) {
          it.artifact(emptyJavadoc)
          it.artifact(javaSources())
        }
      }
    }

    hasAndroid -> {
      createAndroidPublication("release")

      publications.withType(MavenPublication::class.java) {
        it.artifact(emptyJavadoc)
      }
    }

    extensions.findByName("java") != null -> {
      publications.create("default", MavenPublication::class.java) {

        it.from(components.findByName("java"))
        it.artifact(emptyJavadoc)
        it.artifact(javaSources())
      }
    }
  }
}

/**
 * Configures pom
 *
 * @param pomMetadata options for coordinates and POM
 */
fun Project.configurePom(
    pomMetadata: PomMetadata,
) = configurePublishingInternal {
  /**
   * Set `project.group`. This is needed when including builds
   */
  this@configurePom.group = pomMetadata.groupId
  this@configurePom.version = pomMetadata.version

  afterEvaluate {
    publications.configureEach {
      (it as MavenPublication)
      if (it.groupId.isNullOrEmpty()) {
        /**
         * Set the groupId if there is none yet.
         * It might be the case that the publication has a groupId configured already.
         * This is typically the case for Gradle plugins.
         */
        it.groupId = pomMetadata.groupId
      }
      it.artifactId = if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        // Multiplatform -> Keep artifactId untouched
        when {
          it.artifactId == project.name -> project.name
          it.artifactId.startsWith("${project.name}-") -> it.artifactId.replace(Regex("^${project.name}"), pomMetadata.artifactId)
          else -> error("Cannot set artifactId for '${it.artifactId}'")
        }
      } else if (it.artifactId.endsWith("gradle.plugin")) {
        // Gradle plugin marker -> Keep artifactId untouched
        it.artifactId
      } else {
        pomMetadata.artifactId
      }
      it.version = pomMetadata.version

      it.pom {
        it.name.set(name)
        it.description.set(pomMetadata.description)
        it.url.set(pomMetadata.vcsUrl)
        it.scm {
          it.url.set(pomMetadata.vcsUrl)
          it.connection.set(pomMetadata.vcsUrl)
          it.developerConnection.set(pomMetadata.vcsUrl)
        }
        it.licenses {
          it.license {
            /**
             * We set the license name to the SPDX identifier
             * We omit the licenseUrl to play nice with licensee
             * See https://github.com/cashapp/licensee/issues/374
             */
            it.name.set(pomMetadata.license)
          }
        }
        it.developers {
          it.developer {
            it.id.set(pomMetadata.developer)
            it.name.set(pomMetadata.developer)
          }
        }
      }
    }
  }
}

internal fun updateVersionAccordingToEnvironment(version: String): String {
  if (System.getenv("LIBRARIAN_RELEASE") == "true") {
    // This is a release, drop the -SNAPSHOT
    return version.removeSuffix("-SNAPSHOT")
  }

  val sha1 = System.getenv("LIBRARIAN_VERSION_SHA1")
  if (!sha1.isNullOrBlank()) {
    return "$version-${sha1}"
  }

  if(System.getenv("LIBRARIAN_NIGHTLY") == "true") {
    val utcNow = Instant.now().atZone(ZoneOffset.UTC)
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    return "${version.removeSuffix("-SNAPSHOT")}-${utcNow.format(formatter)}"
  }

  return version
}

internal fun DependencyHandler.project(path: String, configuration: String) = project(
    mapOf("path" to path, "configuration" to configuration)
)
