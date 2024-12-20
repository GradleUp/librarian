package com.gradleup.librarian.core.tooling

import com.gradleup.librarian.core.tooling.init.rootPropertiesFilename
import java.io.File

fun tagAndBump(versionToRelease: Version) {
  checkCwd()

  check(runCommand("git", "status", "--porcelain").isEmpty()) {
    "Your git repo is not clean. Make sure to stash or commit your changes before making a release"
  }

  val startBranch = runCommand("git", "symbolic-ref", "--short", "HEAD")
  check(startBranch == "main" || startBranch.startsWith("release-")) {
    "You must be on the main branch or a release branch to make a release"
  }

  val versionToReleaseString = versionToRelease.toString()
  val markdown = processChangelog(versionToReleaseString)

  // 'De-snapshot' the version, open a PR, and merge it
  val releaseBranchName = "tag-$versionToReleaseString"
  runCommand("git", "checkout", "-b", releaseBranchName)
  setCurrentVersion(versionToReleaseString)
  setVersionInDocs(versionToReleaseString)
  runCommand("git", "commit", "-a", "-m", "release $versionToReleaseString")
  runCommand("git", "push", "origin", releaseBranchName)
  runCommand("gh", "pr", "create", "--base", startBranch, "--fill")

  mergeAndWait(releaseBranchName)
  println("Release PR merged.")

  // Tag the release, and push the tag
  runCommand("git", "checkout", startBranch)
  runCommand("git", "pull", "origin", startBranch)
  val tagName = "v$versionToReleaseString"
  runCommand("git", "tag", tagName, "-m", markdown)

  runCommand("git", "push", "origin", tagName)
  println("Tag pushed.")

  // Bump the version to the next snapshot
  val bumpVersionBranchName = "bump-$versionToReleaseString"
  runCommand("git", "checkout", "-b", bumpVersionBranchName)

  val nextSnapshot = versionToRelease.next().copy(isSnapshot = true).toString()
  setCurrentVersion(nextSnapshot)
  runCommand("git", "commit", "-a", "-m", "version is now $nextSnapshot")
  runCommand("git", "push", "origin", bumpVersionBranchName)
  runCommand("gh", "pr", "create", "--base", startBranch, "--fill")

  mergeAndWait(bumpVersionBranchName)
  println("Bump version PR merged.")

  // Go back and pull the changes
  runCommand("git", "checkout", startBranch)
  runCommand("git", "fetch", "-p")
  runCommand("git", "pull", "origin", startBranch)

  println("Everything is done.")
}

internal fun runCommand(vararg args: String): String {
  val builder = ProcessBuilder(*args)
      .redirectError(ProcessBuilder.Redirect.INHERIT)

  val process = builder.start()
  val ret = process.waitFor()

  val output = process.inputStream.bufferedReader().readText()
  check(ret == 0) {
    System.err.flush() //probably not needed?
    "command ${args.joinToString(" ")} failed with output:\n$output"
  }

  return output.trim()
}

internal fun setCurrentVersion(version: String) {
  val file = File(rootPropertiesFilename)
  val newContent = file.readLines().map {
    it.replace(Regex("pom.version=.*"), "pom.version=$version")
  }.joinToString(separator = "\n", postfix = "\n")
  file.writeText(newContent)
}

fun getCurrentVersion(): String {
  val file = File(rootPropertiesFilename)
  require(file.exists()) {
    "Cannot find file ${file.absolutePath}"
  }

  var version: String? = null

  for (line in file.readLines()) {
    val mr = Regex("pom.version=(.*)").matchEntire(line)
    if (mr != null) {
      version = mr.groupValues.get(1)
      break
    }
  }

  require(version != null) {
    "Cannot find pom.version in ${file.absolutePath}"
  }

  return version
}

internal fun setVersionInDocs(version: String) {
  val file = File("Writerside/v.list")
  if (!file.exists()) {
    return
  }

  val regex = Regex("<var *name=\"latest_version\" *instance=\"doc\" *value=\"([^\"]*)\" *type=\"string\" */>")

  file.writeText(file.readText().replace(regex) {
    "<var name=\"latest_version\" instance=\"doc\" value=\"${version}\" type=\"string\" />"
  })
}

internal fun mergeAndWait(branchName: String) {
  runCommand("gh", "pr", "merge", branchName, "--squash", "--auto", "--delete-branch")
  println("Waiting for the PR to be merged...")
  while (true) {
    val state = runCommand("gh", "pr", "view", branchName, "--json", "state", "--jq", ".state")
    if (state == "MERGED") break
    Thread.sleep(1000)
  }
}

internal fun checkCwd() {
  File(".git").apply {
    require(exists() && isDirectory) {
      "No .git folder found. Are you running from the root of the repository?"
    }
  }
}