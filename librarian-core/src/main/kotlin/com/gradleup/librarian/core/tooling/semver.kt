package com.gradleup.librarian.core.tooling

internal fun getNextPatch(version: String): String {
  val components = version.split(".").toMutableList()
  val part = components.removeLast()
  var digitCount = 0
  for (i in part.indices.reversed()) {
    if (part[i] < '0' || part[i] > '9') {
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
  return components.joinToString(".")
}

internal fun getNextSnapshot(version: String): String {
  return getNextPatch(version) + "-SNAPSHOT"
}

/**
 * Mostly used as a sanity check
 */
internal fun String.isValidVersion(): Boolean {
  val c = split(".")
  if (c.size < 3) {
    return false
  }
  for (i in 0.until(3)) {
    if (c[i].toIntOrNull() == null) {
      return false
    }
  }

  return true
}