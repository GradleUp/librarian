package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.gradleup.librarian.core.tooling.runCommand
import kotlin.io.path.Path

internal class TriggerReleaseCommand: CliktCommand(){
  private val versionToRelease by argument()

  override fun run() {
    Path(".").runCommand("gh", "workflow", "run", "tag-and-bump.yaml", "-f", "versionToRelease=$versionToRelease")
  }
}