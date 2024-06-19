package com.gradleup.librarian.core.tooling

import java.io.File
import kotlin.system.exitProcess

internal fun checkCwd() {
  File(".git").apply {
    require(exists() && isDirectory) {
      "No .git folder found. Are you running from the root of the repository?"
    }
  }
}

fun commitRelease(versionToRelease: String) {
  checkCwd()

  check(versionToRelease.isValidVersion()) {
    "Version must start with 'major.minor.patch' (found '$versionToRelease')"
  }

  check(getCurrentVersion().endsWith("-SNAPSHOT")) {
    "Version '${getCurrentVersion()} is not a -SNAPSHOT, check your working directory"
  }

  if (runCommand("git", "status", "--porcelain").isNotEmpty()) {
    println("Your git repo is not clean. Make sure to stash or commit your changes before making a release")
    exitProcess(1)
  }

  processChangelog(versionToRelease)

  val startBranch = runCommand("git", "symbolic-ref", "--short", "HEAD")
  check(startBranch == "main" || startBranch.startsWith("release-")) {
    "You must be on the main branch or a release branch to make a release"
  }

  // 'De-snapshot' the version, open a PR, and merge it
  val releaseBranchName = "$versionToRelease-release"
  runCommand("git", "checkout", "-b", releaseBranchName)
  setCurrentVersion(versionToRelease)
  setVersionInDocs(versionToRelease)
  runCommand("git", "commit", "-a", "-m", "release $versionToRelease")
  runCommand("git", "push", "origin", releaseBranchName)
  runCommand("gh", "pr", "create", "--base", startBranch, "--fill")

  mergeAndWait(releaseBranchName)
  println("Release PR merged.")

  val changelog = extractChangelog(versionToRelease)
  // Tag the release, and push the tag
  runCommand("git", "checkout", startBranch)
  runCommand("git", "pull", "origin", startBranch)
  val tagName = "v$versionToRelease"
  runCommand("git", "tag", tagName, "-m", changelog)

  runCommand("git", "push", "origin", tagName)
  println("Tag pushed.")

  // Bump the version to the next snapshot
  val bumpVersionBranchName = "$versionToRelease-bump-snapshot"
  runCommand("git", "checkout", "-b", bumpVersionBranchName)

  val nextSnapshot = getNextSnapshot(versionToRelease)
  setCurrentVersion(nextSnapshot)
  runCommand("git", "commit", "-a", "-m", "version is now $nextSnapshot")
  runCommand("git", "push", "origin", bumpVersionBranchName)
  runCommand("gh", "pr", "create", "--base", startBranch, "--fill")

  mergeAndWait(bumpVersionBranchName)
  println("Bump version PR merged.")

  // Go back and pull the changes
  runCommand("git", "checkout", startBranch)
  runCommand("git", "pull", "origin", startBranch)

  println("Everything is done.")
}

fun runCommand(vararg args: String): String {
  val builder = ProcessBuilder(*args)
      .redirectError(ProcessBuilder.Redirect.INHERIT)

  val process = builder.start()
  val ret = process.waitFor()

  val output = process.inputStream.bufferedReader().readText()
  if (ret != 0) {
    throw java.lang.Exception("command ${args.joinToString(" ")} failed:\n$output")
  }

  return output.trim()
}

fun setCurrentVersion(version: String) {
  val file = File("librarian.properties")
  val newContent = file.readLines().map {
    it.replace(Regex("pom.version=.*"), "pom.version=$version")
  }.joinToString(separator = "\n", postfix = "\n")
  file.writeText(newContent)
}

fun getCurrentVersion(): String {
  val file = File("librarian.properties")
  require(file.exists()) {
    "Cannot find file ${file.absolutePath}"
  }

  var version: String? = null

  for (line in file.readLines()) {
    val mr = Regex("pom.version=(.*)-SNAPSHOT.*").matchEntire(line)
    if (mr != null) {
      version = mr.groupValues.get(1)
      break
    }
  }

  require(version != null) {
    "Cannot find pom.version in ${file.absolutePath}"
  }

  return "$version-SNAPSHOT"
}

fun setVersionInDocs(version: String) {
  val file = File("Writerside/v.list")
  if (!file.exists()) {
    return
  }

  val regex = Regex("<var *name=\"latest_version\" *instance=\"doc\" *value=\"([^\"]*)\" *type=\"string\" */>")

  file.writeText(file.readText().replace(regex) {
    "<var name=\"latest_version\" instance=\"doc\" value=\"${version}\" type=\"string\" />"
  })
}

fun mergeAndWait(branchName: String) {
  runCommand("gh", "pr", "merge", branchName, "--squash", "--auto", "--delete-branch")
  println("Waiting for the PR to be merged...")
  while (true) {
    val state = runCommand("gh", "pr", "view", branchName, "--json", "state", "--jq", ".state")
    if (state == "MERGED") break
    Thread.sleep(1000)
  }
}


