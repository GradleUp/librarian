package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.gradleup.librarian.core.tooling.prepareRelease

internal class PrepareReleaseCommand: CliktCommand(hidden = true) {
  val versionToRelease by argument()

  override fun run() {
    prepareRelease(versionToRelease)
  }
}
