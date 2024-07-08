package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlin.system.exitProcess

internal class MainCommand: CliktCommand(invokeWithoutSubcommand = true) {
  val version by option().flag()

  init {
    subcommands(PrepareReleaseCommand(), CreateCommand(), ReleaseCommand(), SetupGithubCommand(), InitCommand())
  }

  override fun run() {
    if (version) {
      println("librarian $VERSION")
      exitProcess(1)
    }
  }
}