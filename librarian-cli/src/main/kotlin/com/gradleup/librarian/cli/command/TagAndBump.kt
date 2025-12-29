package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.gradleup.librarian.core.tooling.SemVer
import com.gradleup.librarian.core.tooling.next
import com.gradleup.librarian.core.tooling.getCurrentVersion
import com.gradleup.librarian.core.tooling.nextMinor
import com.gradleup.librarian.core.tooling.nextPatch
import com.gradleup.librarian.core.tooling.runCommand
import com.gradleup.librarian.core.tooling.tagAndBump
import com.gradleup.librarian.core.tooling.semVerOrNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.io.path.Path
import kotlin.system.exitProcess

@OptIn(ExperimentalContracts::class)
internal fun checkOrExit(condition: Boolean, message: () -> String) {
  contract {
    returns() implies condition
  }
  if (!condition) {
    exit(message())
  }
}

internal fun exit(message: String): Nothing {
  println(message)
  exitProcess(1)
}

internal abstract class AbstractTagAndBump : CliktCommand() {
  val versionToRelease by argument().optional()

  abstract fun run(versionToRelease: SemVer)

  private fun confirmOrExit(message: String) {
    if (!KInquirer.promptConfirm(message, true)) {
      exitProcess(1)
    }
  }

  override fun run() {
    val currentVersion = getCurrentVersion()
    val currentVersionParsed = currentVersion.semVerOrNull()
    checkOrExit(currentVersionParsed != null) {
      "Cannot parse current version: '${currentVersion}'"
    }
    checkOrExit(currentVersionParsed.isSnapshot) {
      "Version '${currentVersion} is not a -SNAPSHOT, check your working directory"
    }

    var versionToRelease = this.versionToRelease
    if (versionToRelease == null) {
      val expected = currentVersionParsed.next().toString()
      confirmOrExit("Tag version '${expected}' and bump?")
      versionToRelease = expected
    } else if (versionToRelease == "next") {
      versionToRelease = currentVersionParsed.next().toString()
    } else if (versionToRelease == "patch") {
      versionToRelease = currentVersionParsed.nextPatch().toString()
    } else if (versionToRelease == "minor") {
      versionToRelease = currentVersionParsed.nextMinor().toString()
    }

    val tagVersionParsed = versionToRelease.semVerOrNull()
    checkOrExit(tagVersionParsed != null) {
      "Version '$versionToRelease' cannot be parsed (expected 'major.minor.patch[-prerelease.version][-SNAPSHOT]')"
    }
    if (tagVersionParsed < currentVersionParsed) {
      confirmOrExit("Downgrade version to '${versionToRelease}' and bump? (current version is ${currentVersion})")
    } else if (tagVersionParsed.major > currentVersionParsed.major) {
      confirmOrExit("Bump major version to '${versionToRelease}' and bump? (current version is ${currentVersion})")
    }

    run(tagVersionParsed)
  }
}

internal class TriggerTagAndBump : AbstractTagAndBump() {
  override fun run(versionToRelease: SemVer) {
    Path(".").runCommand("gh", "workflow", "run", "tag-and-bump.yaml", "-f", "versionToRelease=$versionToRelease")
  }
}

internal class TagAndBump : AbstractTagAndBump() {
  override fun run(versionToRelease: SemVer) {
    tagAndBump(versionToRelease)
  }
}