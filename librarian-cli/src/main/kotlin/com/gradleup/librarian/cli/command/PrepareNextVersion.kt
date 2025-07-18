package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File
import java.util.regex.Pattern

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
      acc.replace(Regex("id\\(\"${Pattern.quote(pluginId)}\"\\).version\\([^)]*\\)"), "id(\"$pluginId\").version($version)")
        .replace(Regex("id\\(\"${Pattern.quote(pluginId)}\"\\) version\\([^)]*\\)"), "id(\"$pluginId\") version($version)")
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
