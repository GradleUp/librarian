package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.gradleup.librarian.cli.promptInput
import com.gradleup.librarian.core.tooling.init.initActions
import kotlin.io.path.Path
import kotlin.io.path.exists

internal class ActionsCommand: CliktCommand(help = "Adds .github/workflows yaml files to the current project") {
  val runner by option()

  override fun run() {
    val runner = runner ?: promptInput("Runner", "macos-latest")

    Path(".").apply {
      initActions(runner, resolve("Writerside").exists())
    }
  }
}

