package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import com.gradleup.librarian.core.tooling.init.initCodeStyle
import kotlin.io.path.Path

internal class CodeStyleCommand: CliktCommand(help = "Adds .idea/codeStyle files to the current project") {
  val indent by option()

  override fun run() {
    val indent = indent ?: KInquirer.promptInput("Indent", "4")

    Path(".").apply {
      initCodeStyle(indent)
    }
  }
}
