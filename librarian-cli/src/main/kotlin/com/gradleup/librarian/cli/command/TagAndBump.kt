package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.gradleup.librarian.core.tooling.BumpMajor
import com.gradleup.librarian.core.tooling.DowngradeVersion
import com.gradleup.librarian.core.tooling.UseDefaultVersion
import com.gradleup.librarian.core.tooling.tagAndBump
import kotlin.system.exitProcess

internal class TagAndBump: CliktCommand() {
  val versionToRelease by argument().optional()

  override fun run() {
    tagAndBump(versionToRelease) {
      val ret  = when(it) {
        is BumpMajor -> KInquirer.promptConfirm("Bump major version to '${it.tagVersion}' and bump? (current version is ${it.currentVersion})", true)
        is DowngradeVersion -> KInquirer.promptConfirm("Downgrade version to '${it.tagVersion}' and bump? (current version is ${it.currentVersion})", true)
        is UseDefaultVersion -> KInquirer.promptConfirm("Tag version '${it.expectedVersion}' and bump?", true)
      }

      if (!ret) {
        exitProcess(1)
      }
    }
  }
}