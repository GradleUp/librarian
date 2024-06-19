package com.gradleup.librarian.core.tooling

import java.io.File
import java.time.Instant
import java.time.ZoneOffset

internal val changelogMd = "CHANGELOG.md"
internal val nextVersion = "Next version (unreleased)"

/**
 * Modifies the changelog to:
 * - remove 'Next version (unreleased)'
 * - add the date
 * - add a new 'Next version (unreleased)'
 *
 * @return the changelog for the version to release
 */
fun processChangelog(versionToRelease: String): String {
  checkCwd()

  val file = File(changelogMd)
  require(file.exists()) {
    "No $changelogMd file present."
  }

  val changelog = file.useLines {
    processChangelog(it, versionToRelease)
  }

  file.writeText(changelog.processedChangelog)

  return changelog.versionToReleaseChangelog
}

private enum class ParsingState {
  INTRO,
  ENTRY,
  CONTENT,
}

internal class Changelog(
    val versionToReleaseChangelog: String,
    val processedChangelog: String,
)
internal fun processChangelog(lines: Sequence<String>, versionToRelease: String): Changelog {
  val processedChangelog = StringBuilder()
  val nextVersionMarkdown = StringBuilder()

  var state: ParsingState = ParsingState.INTRO

  run outer@ {
    lines.forEach { line ->
      when (state) {
        ParsingState.INTRO -> {
          if (line.startsWith("# ")) {
            require(line.startsWith("# $nextVersion")) {
              "The first H1 heading of $changelogMd must be '# $nextVersion'."
            }

            processedChangelog.appendLine("# $nextVersion")
            processedChangelog.appendLine()
            processedChangelog.appendLine("PUT_CHANGELOG_HERE")
            processedChangelog.appendLine()
            processedChangelog.appendLine("# Version $versionToRelease")
            processedChangelog.appendLine("_${currentDate()}_")

            state = ParsingState.ENTRY
          } else {
            processedChangelog.appendLine(line)
          }
        }
        ParsingState.ENTRY -> {
          processedChangelog.appendLine(line)
          if (line.startsWith("# ")) {
            state = ParsingState.CONTENT
          } else {
            nextVersionMarkdown.appendLine(line)
          }
        }

        ParsingState.CONTENT -> {
          processedChangelog.appendLine(line)
        }
      }
    }
  }

  if (state == ParsingState.INTRO) {
    error("$changelogMd must contain the next unreleased version:\n'# Version ${'$'}version (unreleased)'")
  }

  return Changelog(
      versionToReleaseChangelog = nextVersionMarkdown.toString(),
      processedChangelog = processedChangelog.toString()
  )
}

private fun currentDate(): String {
  return Instant.now().atOffset(ZoneOffset.UTC).let {
    String.format("%04d-%02d-%02d", it.year, it.monthValue, it.dayOfMonth)
  }
}
