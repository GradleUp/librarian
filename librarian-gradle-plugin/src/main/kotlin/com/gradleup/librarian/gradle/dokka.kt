package com.gradleup.librarian.gradle

import org.gradle.api.Project
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaVersioningPluginParameters
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import javax.inject.Inject


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

internal fun <T> Project.configureDokkaInternal(block: DokkaExtension.() -> T): T {
  plugins.apply("org.jetbrains.dokka")
  return block(extensions.getByType(DokkaExtension::class.java))
}

fun Project.configureDokka() = configureDokkaInternal {
  tasks.withType(DokkaGenerateTask::class.java).configureEach {
    it.workerIsolation.set(ClassLoaderIsolation())
  }
}

fun Project.configureDokkaAggregate(
  currentVersion: String,
  olderVersions: List<Coordinates>,
  aggregateConfiguration: String,
  includeSelf: Boolean,
): TaskProvider<Jar> {
  val jar = configureDokkaInternal {
    dependencies.add(
      "dokkaHtmlPlugin",
      "org.jetbrains.dokka:versioning-plugin:${this.dokkaEngineVersion.get()}"
    )
    dependencies.add(
      "dokkaHtmlPlugin",
      "org.jetbrains.dokka:all-modules-page-plugin:${this.dokkaEngineVersion.get()}"
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

    tasks.withType(DokkaGenerateTask::class.java).configureEach {
      it.dependsOn(downloadKDocVersions)
    }

    /**
     * Strip the /older/ directory to save a bit of space
     */
    tasks.register(kdocWithoutOlder, Jar::class.java) {
      it.archiveClassifier.set("javadoc")
      it.from(
        tasks.named("dokkaGeneratePublicationHtml")
          .map { (it as DokkaGenerateTask).outputDirectory.get().asFile }
      )
      it.exclude("/older/**")
    }
  }

  allprojects {
    if (it == this && !includeSelf) {
      return@allprojects
    }
    dependencies.add("dokka", it)
  }

  configurePublishingInternal {
    publications.create("kdoc", MavenPublication::class.java) {
      it.artifact(jar)
    }
  }

  allprojects.forEach {
    configurations.getByName(aggregateConfiguration).dependencies.add(dependencies.create(it))
  }

  return jar
}

private abstract class FileOperationsHolder @Inject constructor(val fileOperations: FileOperations)

private val kdocWithoutOlder = "kdocWithoutOlder"
