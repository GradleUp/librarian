package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.findGradleProperty
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.Copy
import java.io.File

class LibraryModuleKdoc(
    val aggregator: String,
    val html: KdocHtml?,
) {
  class Builder internal constructor(internal val project: Project) {
    var aggregator: String? = null
    var htmlBuilder: KdocHtml.Builder? = null

    fun html(initialize: KdocHtml.Builder.() -> Unit) {
      if (htmlBuilder == null) {
        htmlBuilder = KdocHtml.Builder(project)
      }
      htmlBuilder?.initialize()
    }

    fun fromGradleProperties() {
      aggregator = project.findGradleProperty("librarian.kdoc.aggregator")
      html { fromGradleProperties() }
    }

    fun build(): LibraryModuleKdoc {
      return LibraryModuleKdoc(
          aggregator ?: error("Librarian: 'aggregator' is required."),
          htmlBuilder?.build()
      )
    }
  }
}


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

class KdocHtml(
    val customStyleSheets: List<File>,
    val customAssets: List<File>,
) {
  class Builder internal constructor(internal val project: Project) {
    var customStyleSheets: List<File>? = null
    var customAssets: List<File>? = null

    fun fromGradleProperties() {
      customStyleSheets = project.findGradleProperty("librarian.kdoc.html.customStyleSheets")?.split(",").orEmpty().map {
        project.rootProject.file(it)
      }
      customAssets = project.findGradleProperty("librarian.kdoc.html.customAssets")?.split(",").orEmpty().map {
        project.rootProject.file(it)
      }
    }

    fun build(): KdocHtml {
      return KdocHtml(
          customStyleSheets.orEmpty(),
          customAssets.orEmpty()
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

fun Project.configureDokkatooModule(libraryModuleKdoc: LibraryModuleKdoc) {
  plugins.apply("dev.adamko.dokkatoo-html")

  configureDokkatooHtml {
    libraryModuleKdoc.html?.customStyleSheets?.let {
      customStyleSheets.from(it)
    }
    libraryModuleKdoc.html?.customAssets?.let {
      customAssets.from(it)
    }
  }
  // TODO project isolation
  val kdocProject = project(":${libraryModuleKdoc.aggregator}")

  kdocProject.configurations.all {
    if (it.name == "dokkatoo") {
      it.dependencies.add(kdocProject.dependencies.project(mapOf("path" to ":${this@configureDokkatooModule.path}")))
    }
  }
}

fun Project.configureDokkatooAggregate(currentVersion: String, olderVersions: List<Coordinates>) {
  plugins.apply("dev.adamko.dokkatoo-html")
  extensions.getByType(DokkatooExtension::class.java).apply {
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

      tasks.register("extractKdocVersion_$versionString", Copy::class.java) {
        it.from(configuration.elements.map { it.map { zipTree(it) } })
        it.into(layout.buildDirectory.dir("kdoc-versions/${version.version}"))
      }
    }

    val downloadKDocVersions = tasks.register("downloadKDocVersions") {
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

internal val kdocWithoutOlder = "kdocWithoutOlder"

internal fun Project.containsTask(name: String): Boolean {
  return try {
    tasks.named(name)
    true
  } catch (e: UnknownTaskException) {
    false
  }
}