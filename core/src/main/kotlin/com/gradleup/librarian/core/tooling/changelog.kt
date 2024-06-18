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
 * @return the version
 */
fun processChangelog(versionToRelease: String){
  checkCwd()

  val file = File(changelogMd)
  require(file.exists()) {
    "No $changelogMd file present."
  }

  val processedChangelog = file.useLines {
    processChangelog(it, versionToRelease)
  }

  file.writeText(processedChangelog)
}

fun extractChangelog(version: String): String {
  checkCwd()

  val file = File(changelogMd)
  require(!file.exists()) {
    "No $changelogMd file present."
  }

  return file.useLines {
    extractChangelog(it, version)
  }
}

/**
 * return the markdown for version 'version'
 */
internal fun extractChangelog(lines: Sequence<String>, version: String): String {
  val markdown = StringBuilder()
  var state: ParsingState = ParsingState.INTRO
  lines.forEach { line ->
    when (state) {
      ParsingState.INTRO -> {
        if (line.startsWith("# ")) {
          val regex = Regex("# Version ([^ ]*) *")
          val matchResult = regex.matchEntire(line)
          if (matchResult == null) {
            // ignore unknown H1 sections
            return@forEach
          }
          if (matchResult.groupValues.get(1) == version) {
            state = ParsingState.CONTENT
          }
        }
      }

      ParsingState.CONTENT -> {
        markdown.appendLine(line)
        if (line.startsWith("# Version")) {
          return markdown.toString()
        }
      }
    }
  }

  require (state != ParsingState.INTRO) {
    "Version '$version' not found"
  }
  return markdown.toString()
}

private enum class ParsingState {
  INTRO,
  CONTENT,
}

internal fun processChangelog(lines: Sequence<String>, versionToRelease: String): String {
  val processedChangelog = StringBuilder()

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

            state = ParsingState.CONTENT
          } else {
            processedChangelog.appendLine(line)
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

  return processedChangelog.toString()
}

private fun currentDate(): String {
  return Instant.now().atOffset(ZoneOffset.UTC).let {
    String.format("%04d-%02d-%02d", it.year, it.monthValue, it.dayOfMonth)
  }
}
