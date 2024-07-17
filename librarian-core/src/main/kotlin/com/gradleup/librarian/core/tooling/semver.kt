package com.gradleup.librarian.core.tooling

class PreRelease(
    val name: String,
    val version: Int,
)


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

  override fun toString(): String {
    return buildString {
      append("$major.$minor.$patch")
      if (preRelease != null) {
        append("-${preRelease.name}.${preRelease.version}")
      }
      if (isSnapshot) {
        append("-SNAPSHOT")
      }
    }
  }
}

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

fun Version.copy(
    major: Int = this.major,
    minor: Int = this.minor,
    patch: Int = this.patch,
    preRelease: PreRelease? = this.preRelease,
    isSnapshot: Boolean = this.isSnapshot,
): Version {
  return Version(
      major,
      minor,
      patch,
      preRelease,
      isSnapshot
  )
}

fun PreRelease.copy(
    name: String = this.name,
    version: Int = this.version
): PreRelease {
  return PreRelease(name, version)
}

fun Version.nextPatch(): Version {
  return copy(patch = patch + 1, preRelease = null, isSnapshot = false)
}

fun Version.nextMinor(): Version {
  return copy(minor = minor + 1, patch = 0, preRelease = null, isSnapshot = false)
}

fun Version.next(): Version {
  return if (preRelease != null) {
    copy(preRelease = preRelease.copy(version = preRelease.version + 1), isSnapshot = false)
  } else {
    copy(patch = patch + 1, isSnapshot = false)
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

  val isSnapshot = rem.endsWith("-SNAPSHOT")
  if (isSnapshot) {
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
      isSnapshot
  )
}
