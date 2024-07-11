package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.gradleup.librarian.core.tooling.tagAndBump

internal class TagAndBump: CliktCommand() {
  val versionToRelease by argument()

  override fun run() {
    tagAndBump(versionToRelease)
  }
}