package com.gradleup.librarian.core.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec


internal fun isTag(): Boolean {
  val ref = System.getenv("GITHUB_REF")

  return ref?.startsWith("refs/tags/") == true
}

internal fun isPullRequest(): Boolean {
  return System.getenv("GITHUB_EVENT_NAME") == "pull_request"
}

internal fun pushedRef(): String? {
  val eventName = System.getenv("GITHUB_EVENT_NAME")
  val ref = System.getenv("GITHUB_REF")

  if (eventName != "push") {
    return null
  }

  return ref
}

fun Project.registerCreateGitHubReleaseTask(block: (Task) ->Unit) {
  tasks.register("librarianCreateGitHubRelease", Exec::class.java) {
    it.commandLine("gh", "release", "create", getTagName(), "--verify-tag", "--notes-from-tag")
    block(it)
  }
}

fun getTagName(): String {
  val ref = System.getenv("GITHUB_REF")
  require (ref != null) {
    "Cannot find GITHUB_REF envitonment variable, are you running from GitHub actions?"
  }
  require(ref.startsWith("refs/tags/")) {
    "Not on a tag? (GITHUB_REF='$ref')"
  }

  return ref.substring("refs/tags/".length)
}