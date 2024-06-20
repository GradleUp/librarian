package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

internal class MainCommand: CliktCommand() {
  init {
    subcommands(PrepareReleaseCommand(), CreateCommand(), ReleaseCommand(), SetupGithubCommand())
  }
  override fun run() {

  }
}