package com.gradleup.librarian.repo

import com.gradleup.librarian.cli.command.VersionContext
import com.gradleup.librarian.cli.command.librarianCurrentVersion
import com.gradleup.librarian.cli.promptChoices
import com.gradleup.librarian.cli.promptSemVer
import com.gradleup.librarian.cli.promptYesNo
import com.gradleup.librarian.core.tooling.*


fun VersionContext.librarianSetVersion() {
  file("librarian.root.properties") {
    replaceRegex(Regex("pom.version=.*")) {
      "pom.version=$version"
    }
  }
}

/**
 * @param setVersion how to set the version. The version is the version of the repo.
 * @param setVersionInDocs how to set the docs version. The docs version is lagging behind the repo version and is not a SNAPSHOT.
 */
fun updateRepo(getVersion: () -> String = ::librarianCurrentVersion, setVersion: VersionContext.() -> Unit = VersionContext::librarianSetVersion, setVersionInDocs: VersionContext.() -> Unit = {}) {
  val setVersion = "Set the version of this project"
  val setVersionInDocs = "Set the version used in docs"
  val prepareNextVersion = "Prepare the next version"
  var choice = promptChoices("Hi \uD83D\uDC4B What do you want to do today?", listOf(prepareNextVersion, setVersion, setVersionInDocs))
  val currentVersion = getVersion().semVerOrNull()
  check(currentVersion != null) {
    "Cannot parse version '$currentVersion'"
  }
  if (!currentVersion.isSnapshot) {
    println("⚠\uFE0F Warning: the current version does not ends with '-SNAPSHOT' (`$currentVersion`).")
  }

  when (choice) {
    setVersion -> {
      VersionContext(promptSemVer().toString()).setVersion()
    }
    setVersionInDocs -> {
      VersionContext(promptSemVer().toString()).setVersionInDocs()
    }

    prepareNextVersion -> {
      val preRelease = if (currentVersion.preRelease != null) { "Bump prerelease: ${currentVersion.nextPrerelease()}" } else { null }
      val nextPatch = "Bump patch     : ${currentVersion.nextPatch()}"
      val nextMinor = "Bump minor     : ${currentVersion.nextMinor()}"
      val nextMajor = "Bump major     : ${currentVersion.nextMajor()}"
      val other = "Other"

      choice = promptChoices("What should be the version?\nCurrent is `$currentVersion`", listOfNotNull(preRelease, nextPatch, nextMinor, nextMajor, other))
      val newVersion = when (choice) {
        preRelease -> currentVersion.nextPrerelease()
        nextPatch -> currentVersion.nextPatch()
        nextMinor -> currentVersion.nextMinor()
        nextMajor -> currentVersion.nextMajor()
        other -> promptSemVer()
        else -> error("Impossible state")
      }

      val versionInDocs = currentVersion.copy(isSnapshot = false)
      VersionContext(newVersion.toString()).setVersion()
      VersionContext(versionInDocs.toString()).setVersionInDocs()

      val commit = promptYesNo("Switch to a new branch and commit the results?")

      println("Version of the project: $newVersion")
      println("Version in docs       : $versionInDocs")

      if (commit) {
        val branch = "bump-to-$newVersion"
        runCommand("git", "checkout", "-b", branch)
        runCommand("git", "commit", "-a", "-m", "Version is now $newVersion")
        println("Switched to a new branch: $branch")
      }
    }
  }
}

fun main(args: Array<String>) {
  updateRepo {}
}