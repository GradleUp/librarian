package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.createAndroidPublication
import com.gradleup.librarian.core.internal.hasAndroid
import com.gradleup.librarian.core.internal.task.CreateRepoTask
import com.gradleup.librarian.core.internal.task.ReleaseRepoTask
import net.mbonnin.vespene.lib.NexusStagingClient
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import java.util.Date
import java.util.Properties

internal const val CONFIGURATION_REPO_ID = "librarianRepoId"
internal const val CONFIGURATION_RELEASE_DATE = "librarianReleaseDate"
internal const val CONFIGURATION_SNAPSHOT_DATE = "librarianSnapshotDate"

internal fun Project.configurePublishingInternal(block: PublishingExtension.() -> Unit) {
  plugins.apply("maven-publish")
  block(extensions.getByType(PublishingExtension::class.java))
}

enum class SonatypeHost {
  Default,
  S01,
}

class Sonatype(
    val username: String?,
    val password: String?,
    val host: SonatypeHost,
)

fun PomMetadata(artifactId: String, properties: Properties): PomMetadata {
  return PomMetadata(
          groupId = properties.getRequiredProperty("pom.groupId"),
          artifactId = artifactId,
          version = properties.getRequiredProperty("pom.version"),
          description = properties.getRequiredProperty("pom.description"),
          vcsUrl = properties.getRequiredProperty("pom.vcsUrl"),
          developer = properties.getRequiredProperty("pom.developer"),
          licenseUrl = properties.getRequiredProperty("pom.licenseUrl"),
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
    val licenseUrl: String,
)

fun Project.configurePublishing(
    createMissingPublications: Boolean,
    publishPlatformArtifactsInRootModule: Boolean,
    pomMetadata: PomMetadata,
    sonatype: Sonatype,
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

  val repoIdConfiguration = configurations.detachedConfiguration(
      dependencies.project(rootProject.path, CONFIGURATION_REPO_ID)
  )

  extensions.getByType(PublishingExtension::class.java).apply {
    repositories {
      it.mavenSonatypeSnapshot(sonatype)
      it.mavenSonatypeStaging(sonatype = sonatype, repoIdConfiguration.elements.map {
        val repoId = it.single().asFile.readText()
        "${sonatype.host.toBaseUrl()}/service/local/staging/deployByRepositoryId/$repoId/"
      })
    }
  }

  tasks.configureEach {
    if (it is AbstractPublishToMaven) {
      if (it.name.endsWith("SonatypeStagingRepository")) {
        // We need that to avoid error like this:
        // Querying the mapped value of provider(java.util.Set) before task ':librarianCreateStagingRepo' has completed is not supported
        it.inputs.files(repoIdConfiguration)
      } else if (it.name.endsWith("SonatypeSnapshotsRepository")) {
        it.enabled = pomMetadata.version.endsWith("-SNAPSHOT")
      }
    }
  }

  val releaseConfiguration = configurations.create(CONFIGURATION_RELEASE_DATE)
  val snapshotConfiguration = configurations.create(CONFIGURATION_SNAPSHOT_DATE)

  tasks.named("publishAllPublicationsToSonatypeStagingRepository").apply {
    configure { task ->
      task.outputs.file(layout.buildDirectory.file("${task.name}Date.txt"))
      task.doLast {
        it.outputs.files.singleFile.writeText("${it.name}: ${Date()}")
      }
    }
    artifacts {
      it.add(releaseConfiguration.name, this)
    }
  }
  tasks.named("publishAllPublicationsToSonatypeSnapshotsRepository").apply {
    configure { task ->
      task.outputs.file(layout.buildDirectory.file("${task.name}Date.txt"))
      task.doLast {
        it.outputs.files.singleFile.writeText("${it.name}: ${Date()}")
      }
    }
    artifacts {
      it.add(snapshotConfiguration.name, this)
    }
  }
}

private fun Project.emptyJavadoc(repositoryUrl: String?): TaskProvider<Jar> {
  return tasks.register("librarianEmptyJavadoc", org.gradle.jvm.tasks.Jar::class.java) {
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
       * Kotlin MPP creates publications.
       * It only misses the javadoc
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

fun Project.configureRepositoriesRoot(sonatype: Sonatype, createRepoTask: TaskProvider<CreateRepoTask>) = configurePublishingInternal {
  repositories {
    it.mavenSonatypeSnapshot(sonatype)
    it.mavenSonatypeStaging(sonatype = sonatype, createRepoTask.map {
      val repoId = it.output.asFile.get().readText()
      "${sonatype.host.toBaseUrl()}/service/local/staging/deployByRepositoryId/$repoId/"
    })
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
      it.groupId = pomMetadata.groupId
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
            it.name.set(pomMetadata.license)
            it.url.set(pomMetadata.licenseUrl)
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

internal fun SonatypeHost.toBaseUrl(): String {
  return when (this) {
    SonatypeHost.S01 -> "https://s01.oss.sonatype.org"
    SonatypeHost.Default -> "https://oss.sonatype.org"
  }
}

private fun RepositoryHandler.mavenSonatypeSnapshot(
    sonatype: Sonatype,
) = maven {
  it.name = "SonatypeSnapshots"
  it.setUrl("${sonatype.host.toBaseUrl()}/content/repositories/snapshots/")
  it.credentials {
    it.username = sonatype.username
    it.password = sonatype.password
  }
}

internal fun RepositoryHandler.mavenSonatypeStaging(
    sonatype: Sonatype,
    url: Provider<String>,
) = maven {
  it.name = "SonatypeStaging"
  it.setUrl(url)
  it.credentials {
    it.username = sonatype.username
    it.password = sonatype.password
  }
}

internal fun TaskContainer.providerByName(name: String): TaskProvider<Task>? {
  return try {
    this.named(name)
  } catch (e: UnknownTaskException) {
    null
  }
}

internal fun DependencyHandler.project(path: String, configuration: String) = project(
    mapOf("path" to path, "configuration" to configuration)
)

internal fun Project.registerCreateRepoIdTask(
    sonatype: Sonatype,
    group: String,
    version: String,
): TaskProvider<CreateRepoTask> {
  return rootProject.tasks.register("librarianCreateStagingRepo", CreateRepoTask::class.java) {
    it.output.set(rootProject.layout.buildDirectory.file("librarianRepoId"))
    it.repoDescription.set("$group:${project.name}:$version")
    it.sonatypeHost.set(sonatype.host)
    it.groupId.set(group)
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
  }
}

internal fun nexusStagingClient(host: SonatypeHost, username: String, password: String): NexusStagingClient {
  return NexusStagingClient(
      baseUrl = "${host.toBaseUrl()}/service/local/",
      username = username,
      password = password
  )
}

internal fun Project.registerReleaseTask(
    sonatype: Sonatype,
    repoId: Provider<String>,
): TaskProvider<out Task> {
  return tasks.register("librarianReleaseStagingRepo", ReleaseRepoTask::class.java) {
    it.host.set(sonatype.host)
    it.username.set(sonatype.username)
    it.password.set(sonatype.password)
    it.repoId.set(repoId)
  }
}


fun <T : Task> TaskProvider<T>.dependsOn(other: Any) {
  configure {
    it.dependsOn(other)
  }
}
