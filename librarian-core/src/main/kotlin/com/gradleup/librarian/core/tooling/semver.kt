package com.gradleup.librarian.core.tooling

class PreRelease(
    val name: String,
    val version: Int,
)

/**
 *
 * A variation of the traditional semantic versioning specification to support `-SNAPSHOT` and prerelease versions.
 *
 * ```
 * <valid semver> ::= <version core>
 *                  | <version core> "-SNAPSHOT"
 *                  | <version core> "-" <pre-release>
 *                  | <version core> "-" <pre-release> "-SNAPSHOT"
 *
 * <version core> ::= <major> "." <minor> "." <patch>
 *
 * <major> ::= <numeric identifier>
 *
 * <minor> ::= <numeric identifier>
 *
 * <patch> ::= <numeric identifier>
 *
 * <pre-release> ::= <dot-separated pre-release identifiers>
 *
 * <dot-separated pre-release identifiers> ::= <pre-release identifier>
 *                                           | <pre-release identifier> "." <dot-separated pre-release identifiers>
 *
 * <build> ::= <dot-separated build identifiers>
 *
 * <dot-separated build identifiers> ::= <build identifier>
 *                                     | <build identifier> "." <dot-separated build identifiers>
 *
 * <pre-release identifier> ::= <alphanumeric identifier>
 *                            | <numeric identifier>
 *
 * <alphanumeric identifier> ::= <non-digit>
 *                             | <non-digit> <identifier characters>
 *                             | <identifier characters> <non-digit>
 *                             | <identifier characters> <non-digit> <identifier characters>
 *
 * <numeric identifier> ::= "0"
 *                        | <positive digit>
 *                        | <positive digit> <digits>
 *
 * <identifier characters> ::= <identifier character>
 *                           | <identifier character> <identifier characters>
 *
 * <identifier character> ::= <digit>
 *                          | <non-digit>
 *
 * <non-digit> ::= <letter>
 *               | "-"
 *
 * <digits> ::= <digit>
 *            | <digit> <digits>
 *
 * <digit> ::= "0"
 *           | <positive digit>
 *
 * <positive digit> ::= "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
 *
 * <letter> ::= "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" | "J"
 *            | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" | "S" | "T"
 *            | "U" | "V" | "W" | "X" | "Y" | "Z" | "a" | "b" | "c" | "d"
 *            | "e" | "f" | "g" | "h" | "i" | "j" | "k" | "l" | "m" | "n"
 *            | "o" | "p" | "q" | "r" | "s" | "t" | "u" | "v" | "w" | "x"
 *            | "y" | "z"
 * ```
 */
class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: PreRelease?,
    val isSnapshot: Boolean,
) {
  operator fun compareTo(currentVersionParsed: SemVer): Int {
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
    // XXX: should we handle non-lexicographic order here? (-dev could be lower than -alpha, etc...)
    val ret = name.compareTo(other.name)
    if (ret != 0) {
      return ret
    }

    version.compareTo(other.version)
  }
}

fun SemVer.copy(
    major: Int = this.major,
    minor: Int = this.minor,
    patch: Int = this.patch,
    preRelease: PreRelease? = this.preRelease,
    isSnapshot: Boolean = this.isSnapshot,
): SemVer {
  return SemVer(
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

fun SemVer.nextPatch(): SemVer {
  if (preRelease != null) {
    return copy(preRelease = null)
  }
  return copy(patch = patch + 1, preRelease = null)
}

fun SemVer.nextMinor(): SemVer {
  return copy(minor = minor + 1, patch = 0, preRelease = null)
}

fun SemVer.nextMajor(): SemVer {
  return copy(major = major + 1, minor = 0, patch = 0, preRelease = null)
}

fun SemVer.nextPrerelease(): SemVer {
  require(preRelease != null) { "Cannot bump prerelease on non-prerelease version" }
  return copy(preRelease = preRelease.copy(version = preRelease.version + 1))
}

/**
 * Bumps the prerelease if any, or the patch else.
 */
fun SemVer.bump(): SemVer {
  return when {
    preRelease != null -> nextPrerelease()
    else -> nextPatch()
  }
}

@Deprecated("Use bump() instead")
fun SemVer.next(): SemVer {
  return when {
    isSnapshot -> {
      copy(isSnapshot = false)
    }
    preRelease != null -> {
      copy(preRelease = preRelease.copy(version = preRelease.version + 1), isSnapshot = false)
    }
    else -> {
      copy(patch = patch + 1, isSnapshot = false)
    }
  }
}

fun String.semVerOrNull(): SemVer? {
  val regex1 = Regex("([0-9]+)\\.([0-9]+)\\.([0-9]+)(.*)")

  val result1 = regex1.matchEntire(this) ?: return null

  val major = result1.groupValues[1].toIntOrNull() ?: return null
  val minor = result1.groupValues[2].toIntOrNull() ?: return null
  val patch = result1.groupValues[3].toIntOrNull() ?: return null

  var rem = result1.groupValues[4]
  if (rem.isEmpty()) {
    return SemVer(major, minor, patch, null, false)
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
  return SemVer(
      major,
      minor,
      patch,
      preRelease,
      isSnapshot
  )
}
