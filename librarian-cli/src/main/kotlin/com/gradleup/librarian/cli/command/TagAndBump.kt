package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.gradleup.librarian.core.tooling.BumpMajor
import com.gradleup.librarian.core.tooling.DowngradeVersion
import com.gradleup.librarian.core.tooling.UseDefaultVersion
import com.gradleup.librarian.core.tooling.getCurrentVersion
import com.gradleup.librarian.core.tooling.tagAndBump
import com.gradleup.librarian.core.tooling.toVersionOrNull
import kotlin.system.exitProcess

internal class TagAndBump: CliktCommand() {
  val versionToRelease by argument().optional()
  val patch by option().flag()
  val minor by option().flag()

  private fun confirmOrExit(message: String) {
    if (!KInquirer.promptConfirm(message, true)) {
      exitProcess(1)
    }
  }

  override fun run() {
    val currentVersion = getCurrentVersion()
    val currentVersionParsed = currentVersion.toVersionOrNull()
    check(currentVersionParsed != null) {
      "Cannot parse current version: '${currentVersion}'"
    }
    check(currentVersionParsed.isSnapshot) {
      "Version '${currentVersion} is not a -SNAPSHOT, check your working directory"
    }

    val expectedVersion = currentVersion.removeSuffix("-SNAPSHOT")

    var versionToRelease = this.versionToRelease
    if (versionToRelease == null) {
      confirmOrExit("Tag version '${expectedVersion}' and bump?")
      versionToRelease = expectedVersion
    }

    val tagVersionParsed = versionToRelease.toVersionOrNull()
    check(tagVersionParsed != null) {
      "Version must start with 'major.minor.patch' (found '$versionToRelease')"
    }
    if (tagVersionParsed < currentVersionParsed) {
      confirmOrExit("Downgrade version to '${versionToRelease}' and bump? (current version is ${currentVersion})")
    } else if (tagVersionParsed.major > currentVersionParsed.major) {
      confirmOrExit("Bump major version to '${versionToRelease}' and bump? (current version is ${currentVersion})")
    }

    tagAndBump(versionToRelease)
  }
}