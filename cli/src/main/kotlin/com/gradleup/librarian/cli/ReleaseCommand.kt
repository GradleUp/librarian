package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.gradleup.librarian.core.tooling.commitRelease

class ReleaseCommand: CliktCommand() {
  val version by option().required().help("The version to release")

  override fun run() {
    commitRelease(version)
  }
}
