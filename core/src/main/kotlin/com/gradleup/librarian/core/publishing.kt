package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.createAndroidPublication
import com.gradleup.librarian.core.internal.configurationLibrarianRepositoryId
import com.gradleup.librarian.core.internal.findEnvironmentVariable
import com.gradleup.librarian.core.internal.findGradleProperty
import com.gradleup.librarian.core.internal.hasKotlin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

/**
 * Applies the `maven-publish` plugin and executes [block].
 */
fun Project.configurePublishing(block: PublishingExtension.() -> Unit) {
  plugins.apply("maven-publish")
  block(extensions.getByType(PublishingExtension::class.java))
}

class Publishing(
    val createMissingPublications: Boolean,
    val publishPlatformArtifactsInRootModule: Boolean,
    val pomMetadata: PomMetadata,
    val sonatype: Sonatype,
    val signing: Signing,
) {

  class Builder internal constructor(internal val project: Project) {
    var pomMetadataBuilder: PomMetadata.Builder? = null
    var sonatypeBuilder: Sonatype.Builder = Sonatype.Builder(project)
    var signingBuilder: Signing.Builder = Signing.Builder(project)
    var createMissingPublications = true
    var publishPlatformArtifactsInRootModule = true

    fun pom(initialize: PomMetadata.Builder.() -> Unit) {
      if (pomMetadataBuilder == null) {
        pomMetadataBuilder = PomMetadata.Builder(project)
      }
      pomMetadataBuilder?.initialize()
    }

    fun sonatype(initialize: Sonatype.Builder.() -> Unit) {
      sonatypeBuilder.initialize()
    }

    fun signing(initialize: Signing.Builder.() -> Unit) {
      signingBuilder.initialize()
    }

    fun fromGradlePropertiesAndEnvironmentVariables() {
      pom { fromGradleProperties() }

      sonatype { fromGradlePropertiesAndEnvironmentVariables() }

      if (project.findGradleProperty("librarian.publishing.signing")?.toBooleanStrictOrNull() != false) {
        signing { fromEnvironmentVariables() }
      }

       project.findGradleProperty("librarian.publishing.publishPlatformArtifactsInRootModule")?.toBooleanStrict()?.let {
         publishPlatformArtifactsInRootModule = it
       }

      project.findGradleProperty("librarian.publishing.createMissingPublications")?.toBooleanStrict()?.let {
        createMissingPublications = it
      }
    }

    fun build(): Publishing {
      return Publishing(
          pomMetadata = pomMetadataBuilder?.build() ?: error("Librarian: 'pom' is required."),
          sonatype = sonatypeBuilder.build(),
          signing = signingBuilder.build(),
          createMissingPublications = createMissingPublications,
          publishPlatformArtifactsInRootModule = publishPlatformArtifactsInRootModule,
      )
    }
  }
}

enum class SonatypeHost {
  Default,
  S01,
}

class Sonatype(
    val username: String?,
    val password: String?,
    val host: SonatypeHost,
    val stagingProfile: String?,
) {
  class Builder internal constructor(internal val project: Project) {
    var username: String? = null
    var password: String? = null
    var host: SonatypeHost? = null
    /**
     * The staging profile if you have several. If null, uses the single staging profile associated with the account.
     *
     * Get your staging profiles at https://s01.oss.sonatype.org/#stagingRepositories
     */
    var stagingProfile: String? = null

    fun fromGradlePropertiesAndEnvironmentVariables() {
      host = project.findGradleProperty("librarian.sonatype.host")?.let { SonatypeHost.valueOf(it) }
      username = project.findEnvironmentVariable("LIBRARIAN_SONATYPE_USERNAME")
      password = project.findEnvironmentVariable("LIBRARIAN_SONATYPE_PASSWORD")
      stagingProfile = project.findEnvironmentVariable("LIBRARIAN_SONATYPE_STAGING_PROFILE")
    }

    fun build(): Sonatype {
      return Sonatype(
          username = username,
          password = password,
          host = host ?: error("Librarian: 'host' is required."),
          stagingProfile = stagingProfile,
      )
    }
  }
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
) {
  class Builder internal constructor(internal val project: Project) {
    var groupId: String? = null
    var artifactId: String? = null
    var version: String? = null
    var description: String? = null
    var vcsUrl: String? = null
    var developer: String? = null
    var license: String? = null
    var licenseUrl: String? = null

    fun fromGradleProperties() {
      groupId = project.findGradleProperty("librarian.pom.groupId")
      artifactId = project.name
      version = project.findGradleProperty("librarian.pom.version")
      description = project.findGradleProperty("librarian.pom.description")
      vcsUrl = project.findGradleProperty("librarian.pom.vcsUrl")
      developer = project.findGradleProperty("librarian.pom.developer")
      license = project.findGradleProperty("librarian.pom.license")
      licenseUrl = project.findGradleProperty("librarian.pom.licenseUrl")
    }

    fun build(): PomMetadata {
      return PomMetadata(
          groupId = groupId ?: error("Librarian: 'groupId' is required."),
          artifactId = artifactId ?: error("Librarian: 'artifactId' is required."),
          version = version ?: error("Librarian: 'version' is required."),
          description = description ?: error("Librarian: 'description' is required."),
          vcsUrl = vcsUrl ?: error("Librarian: 'vcsUrl' is required."),
          developer = developer ?: error("Librarian: 'developer' is required."),
          license = license ?: error("Librarian: 'license' is required."),
          licenseUrl = licenseUrl ?: error("Librarian: 'licenseUrl' is required."),
      )
    }
  }
}

fun Project.configurePublishing(
    publishing: Publishing
) {
  if (publishing.createMissingPublications) {
    createMissingPublications(publishing.pomMetadata.vcsUrl)
  }
  configurePom(publishing.pomMetadata)
  if (publishing.publishPlatformArtifactsInRootModule) {
    if (hasKotlin) {
      publishPlatformArtifactsInRootModule()
    }
  }
  publishing.sonatype.let {
    configureRepositories(it)
  }
  publishing.signing.let {
    configureSigning(it)
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
) = configurePublishing {
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

    extensions.findByName("android") != null -> {
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

    tasks.providerByName(kdocWithoutOlder) != null -> {
      val kdocWithoutOlder = tasks.providerByName(kdocWithoutOlder)

      publications.create("default", MavenPublication::class.java) {
        it.artifact(kdocWithoutOlder)
      }
    }
  }
}

/**
 * Configures the sonatype repositories
 */
fun Project.configureRepositories(sonatype: Sonatype) = configurePublishing {
  val configuration = configurations.detachedConfiguration(
      dependencies.project(
          mapOf(
              "path" to rootProject.path,
              "configuration" to configurationLibrarianRepositoryId
          )
      )
  )

  repositories {
    it.mavenSonatypeSnapshot(sonatype)
    it.mavenSonatypeStaging(sonatype = sonatype, configuration.elements.map {
      val repoId = it.single().asFile.readText()
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
) = configurePublishing {
  afterEvaluate {
    publications.configureEach {
      (it as MavenPublication)
      it.groupId = pomMetadata.groupId
      it.artifactId = if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        when {
          it.artifactId == project.name -> project.name
          it.artifactId.startsWith("${project.name}-") -> it.artifactId.replace(Regex("^${project.name}"), pomMetadata.artifactId)
          else -> error("Cannot set artifactId for '${it.artifactId}'")
        }
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
