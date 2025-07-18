package com.gradleup.librarian.cli.command

import java.io.File


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
