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

class PreRelease(
    val name: String,
    val version: Int,
)

internal fun PreRelease?.compareTo(other: PreRelease?): Int {
  return if (this == null && other == null) {
    0
  } else if (this == null) {
    1
  } else if (other == null) {
    -1
  } else {
    // XXX: should we handle non-lexicographic order here?
    val ret = name.compareTo(other.name)
    if (ret != 0) {
      return ret
    }

    return version.compareTo(other.version)
  }
}

class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: PreRelease?,
    val isSnapshot: Boolean,
) {
  operator fun compareTo(currentVersionParsed: Version): Int {
    var ret = major.compareTo(currentVersionParsed.major)
    if (ret != 0) return ret

    ret = minor.compareTo(currentVersionParsed.minor)
    if (ret != 0) return ret

    ret = patch.compareTo(currentVersionParsed.patch)
    if (ret != 0) return ret

    ret = preRelease.compareTo(currentVersionParsed.preRelease)
    if (ret != 0) return ret

    ret = major.compareTo(currentVersionParsed.major)
    if (ret != 0) return ret

    return 0
  }
}

fun String.toVersionOrNull(): Version? {
  val regex1 = Regex("([0-9]+)\\.([0-9]+)\\.([0-9]+)(.*)")

  val result1 = regex1.matchEntire(this) ?: return null

  val major = result1.groupValues[1].toIntOrNull() ?: return null
  val minor = result1.groupValues[2].toIntOrNull() ?: return null
  val patch = result1.groupValues[3].toIntOrNull() ?: return null

  var rem = result1.groupValues[4]
  if (rem.isEmpty()) {
    return Version(major, minor, patch, null, false)
  }

  var preRelease: PreRelease? = null

  val snapshot = rem.endsWith("-SNAPSHOT")
  if (snapshot) {
    rem = rem.removeSuffix("-SNAPSHOT")
  }
  if (rem.isNotEmpty()) {
    if (!rem.startsWith("-")) {
      return null
    }
    rem = rem.substring(1)
    val regex2 = Regex("([a-zA-Z]+)\\.([0-9]+)")
    val result2 = regex2.matchEntire(rem) ?: return null

    val v = result2.groupValues[2].toIntOrNull() ?: return null
    preRelease = PreRelease(result2.groupValues[1], v)
  }
  return Version(
      major,
      minor,
      patch,
      preRelease,
      snapshot
  )
}
