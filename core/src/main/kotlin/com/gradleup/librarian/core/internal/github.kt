package com.gradleup.librarian.core.internal


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
