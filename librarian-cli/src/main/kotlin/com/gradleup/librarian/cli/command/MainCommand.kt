package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.gradleup.librarian.cli.VERSION
import kotlin.system.exitProcess

internal class MainCommand: CliktCommand(invokeWithoutSubcommand = true) {
  val version by option().flag()

  init {
    subcommands(Create(), InitCommand(), TagAndBump(), TriggerTagAndBump(), )
  }

  override fun run() {
    if (version) {
      println("librarian $VERSION")
      exitProcess(0)
    }
    val subcommand = currentContext.invokedSubcommand
    if (subcommand == null) {
      println(getFormattedHelp())
      exitProcess(1)
    }
  }
}