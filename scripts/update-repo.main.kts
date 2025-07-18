#!/usr/bin/env kotlin

@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.2")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import java.io.File
import java.util.regex.Pattern

class MainCommand : CliktCommand() {
    override fun run() {
    }
}

class PrepareNextVersion(private val setVersionInDocs: VersionContext.() -> Unit) : CliktCommand() {
  override fun run() {
    val currentVersion = getCurrentVersion()
    check(currentVersion.endsWith("-SNAPSHOT")) {
      "Current version '$currentVersion' does not ends with '-SNAPSHOT'. Call set-version to update it."
    }

    val releaseVersion = currentVersion.dropSnapshot()
    val nextSnapshot = getNextSnapshot(releaseVersion)

    VersionContext(releaseVersion).setVersionInDocs()
    setCurrentVersion(nextSnapshot)

    println("Docs have been updated to use version '$releaseVersion'.")
    println("Version is now '$nextSnapshot'.")
  }
}

class VersionContext(val version: String) {
  fun file(path: String, block: FileContext.() -> Unit) {
    val f = File(path)
    var newText = f.readText()

    val fileContext = FileContext().also(block)
    newText = fileContext.gas.fold(newText) { acc, ga ->
      acc.replace(Regex("\"${Pattern.quote(ga)}:.*\""), "\"$ga:$version\"")
    }
    newText = fileContext.pluginIds.fold(newText) { acc, pluginId ->
      acc.replace(Regex("id\\(\"${Pattern.quote(pluginId)}\"\\).version\\([^\\)]*\\)"), "id(\"$pluginId\").version($version)")
        .replace(Regex("id\\(\"${Pattern.quote(pluginId)}\"\\) version\\([^\\)]*\\)"), "id(\"$pluginId\") version($version)")
    }

    f.writeText(newText)
  }
}

class FileContext {
  internal val gas = mutableListOf<String>()
  internal val pluginIds = mutableListOf<String>()

  /**
   * Replaces every instance version with the new version in all instances of:
   *
   * - `"$ga:version"`
   *
   * @param ga a group and artifact ids in the form `group:artifact`
   */
  fun replaceMavenCoordinates(ga: String) {
    gas.add(ga)
  }

  /**
   * Replaces every instance version with the new version in all instances of:
   *
   * - `id("$pluginId").version(version)`
   * - `id("$pluginId") version(version)`
   */
  fun replacePluginId(pluginId: String) {
    pluginIds.add(pluginId)
  }
}

internal fun String.dropSnapshot() = this.removeSuffix("-SNAPSHOT")

internal fun setCurrentVersion(version: String) {
  val librarianRootProperties = File("librarian.root.properties")
  val newContent = librarianRootProperties.readLines().joinToString(separator = "\n", postfix = "\n") {
    it.replace(Regex("pom.version=.*"), "pom.version=$version")
  }
  librarianRootProperties.writeText(newContent)
}

internal fun getCurrentVersion(): String {
  val versionLines = File("librarian.root.properties").readLines().filter { it.startsWith("pom.version=") }

  require(versionLines.size > 0) {
    "cannot find the version in ./librarian.root.properties"
  }

  require(versionLines.size == 1) {
    "multiple versions found in ./librarian.root.properties"
  }

  val regex = Regex("pom.version=(.*)-SNAPSHOT")
  val matchResult = regex.matchEntire(versionLines.first())

  require(matchResult != null) {
    "'${versionLines.first()}' doesn't match pom.version=(.*)-SNAPSHOT"
  }

  return matchResult.groupValues[1] + "-SNAPSHOT"
}

internal fun getNextSnapshot(version: String): String {
  val components = version.split(".").toMutableList()
  val part = components.removeLast()
  var digitCount = 0
  for (i in part.indices.reversed()) {
    if (part[i] !in '0'..'9') {
      break
    }
    digitCount++
  }

  check(digitCount > 0) {
    "Cannot find a number to bump in $version"
  }

  // prefix can be "alpha", "dev", etc...
  val prefix = if (digitCount < part.length) {
    part.substring(0, part.length - digitCount)
  } else {
    ""
  }
  val numericPart = part.substring(part.length - digitCount, part.length)
  val asNumber = numericPart.toInt()

  val nextPart = if (numericPart[0] == '0') {
    // https://docs.gradle.org/current/userguide/single_versions.html#version_ordering
    // Gradle understands that alpha2 > alpha10 but it might not be the case for everyone so
    // use the same naming schemes as other libs and keep the prefix
    val width = numericPart.length
    String.format("%0${width}d", asNumber + 1)
  } else {
    (asNumber + 1).toString()
  }

  components.add("$prefix$nextPart")
  return components.joinToString(".") + "-SNAPSHOT"
}

MainCommand().subcommands(PrepareNextVersion {
  file("README.md") {
    replacePluginId("com.gradleup.librarian")
  }
}).main(args)
