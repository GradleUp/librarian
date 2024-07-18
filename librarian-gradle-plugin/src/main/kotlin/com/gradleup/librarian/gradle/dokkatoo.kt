package com.gradleup.librarian.gradle

import com.gradleup.librarian.gradle.internal.findGradleProperty
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import java.io.File
import javax.inject.Inject

class KdocAggregate(
    val currentVersion: String,
    val olderVersions: List<Coordinates>,
) {
  class Builder internal constructor(internal val project: Project) {
    var olderVersions: List<Coordinates>? = null
    var currentVersion: String? = null

    fun fromGradleProperties() {
      currentVersion = project.findGradleProperty("librarian.pom.version")
      olderVersions = project.findGradleProperty("librarian.kdoc.olderVersions")
          ?.split(",")
          .orEmpty()
          .filter {
            it.isNotEmpty()
          }
          .map {
            Coordinates(it)
          }
    }

    fun build(): KdocAggregate {
      return KdocAggregate(
          currentVersion = currentVersion ?: error("Librarian: 'currentVersion' is required."),
          olderVersions = olderVersions.orEmpty()
      )
    }
  }
}

internal fun Project.configureDokkatooHtml(block: DokkaHtmlPluginParameters.() -> Unit = {}) {
  plugins.apply("dev.adamko.dokkatoo-html")
  extensions.getByType(DokkatooExtension::class.java).apply {
    tasks.withType(DokkatooGenerateTask::class.java).configureEach {
      it.workerIsolation.set(ClassLoaderIsolation())
    }

    dokkatooSourceSets.configureEach {
      it.includes.from("README.md")
    }
    pluginsConfiguration.getByName("html") {
      (it as DokkaHtmlPluginParameters).block()
    }

    // Workaround for https://github.com/adamko-dev/dokkatoo/issues/165
    configurations.configureEach {
      if (it.name.lowercase().contains("dokkatooHtmlPublicationPluginClasspathApiOnlyConsumable".lowercase())) {
        it.attributes {
          it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "poison"))
        }
      }
    }
  }
}

class Coordinates(val groupId: String, val artifactId: String, val version: String) {
  override fun toString(): String {
    return "$groupId:$artifactId:$version"
  }
}

fun Coordinates(coordinates: String): Coordinates {
  return coordinates.split(":").let {
    require(it.size == 3) {
      "Librarian: invalid coordinates: '$coordinates'"
    }
    Coordinates(it[0], it[1], it[2])
  }
}

internal fun <T> Project.configureDokkatooInternal(block: DokkatooExtension.() -> T): T {
  plugins.apply("dev.adamko.dokkatoo-html")
  return block(extensions.getByType(DokkatooExtension::class.java))
}

fun Project.configureDokkatoo() = configureDokkatooInternal {
  tasks.withType(DokkatooGenerateTask::class.java).configureEach {
    it.workerIsolation.set(ClassLoaderIsolation())
  }

  // TODO project isolation
  if (!skipProjectIsolationIncompatibleParts) {
    rootProject.configurations.all {
      if (it.name == "dokkatoo") {
        it.dependencies.add(rootProject.dependencies.project(mapOf("path" to ":${this@configureDokkatoo.path}")))
      }
    }
  }
}

fun Project.configureDokkatooAggregate(currentVersion: String, olderVersions: List<Coordinates>): TaskProvider<Jar> {
  return configureDokkatooInternal {
    plugins.apply("dev.adamko.dokkatoo-html")
    dependencies.add(
        "dokkatooPluginHtml",
        versions.jetbrainsDokka.map { dokkaVersion ->
          "org.jetbrains.dokka:all-modules-page-plugin:$dokkaVersion"
        }
    )
    dependencies.add(
        "dokkatooPluginHtml",
        versions.jetbrainsDokka.map { dokkaVersion ->
          "org.jetbrains.dokka:versioning-plugin:$dokkaVersion"
        }
    )

    val kdocVersionTasks = olderVersions.map { version ->
      val versionString = version.version.replace(".", "_").replace("-", "_")
      val configuration = configurations.create("KdocVersion_$versionString") {
        it.isCanBeResolved = true
        it.isCanBeConsumed = false
        it.isTransitive = false

        it.dependencies.add(project.dependencies.create("$version:javadoc"))
      }

      val fileOperations = objects.newInstance(FileOperationsHolder::class.java).fileOperations

      tasks.register("librarianExtractKdocVersion_$versionString", Copy::class.java) {
        it.from(configuration.elements.map { it.map { fileOperations.zipTree(it) } })
        it.into(layout.buildDirectory.dir("kdoc-versions/${version.version}"))
      }
    }

    val downloadKDocVersions = tasks.register("librarianDownloadKDocVersions") {
      it.dependsOn(kdocVersionTasks)
      it.outputs.dir(layout.buildDirectory.dir("kdoc-versions/"))
      it.doLast {
        // Make sure the folder is created
        it.outputs.files.singleFile.mkdirs()
      }
    }

    pluginsConfiguration.getByName("versioning") {
      it as DokkaVersioningPluginParameters
      it.version.set(currentVersion)
      it.olderVersionsDir.fileProvider(downloadKDocVersions.map { it.outputs.files.singleFile })
    }

    tasks.withType(DokkatooGenerateTask::class.java).configureEach {
      it.dependsOn(downloadKDocVersions)
    }

    /**
     * Strip the /older/ directory to save a bit of space
     */
    tasks.register(kdocWithoutOlder, org.gradle.jvm.tasks.Jar::class.java) {
      it.archiveClassifier.set("javadoc")
      it.from(
          tasks.named("dokkatooGeneratePublicationHtml")
              .map { (it as DokkatooGenerateTask).outputDirectory.get().asFile })
      it.exclude("/older/**")
    }
  }
}

private abstract class FileOperationsHolder @Inject constructor(val fileOperations: FileOperations)

private val kdocWithoutOlder = "kdocWithoutOlder"

internal fun Project.containsTask(name: String): Boolean {
  return try {
    tasks.named(name)
    true
  } catch (e: UnknownTaskException) {
    false
  }
}