package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.gradleup.librarian.core.tooling.prepareRelease

internal class PrepareReleaseCommand: CliktCommand(hidden = true) {
  val versionToRelease by option().required().help("The version to release")

  override fun run() {
    prepareRelease(versionToRelease)
  }
}
