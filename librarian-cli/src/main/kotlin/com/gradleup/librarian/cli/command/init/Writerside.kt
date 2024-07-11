package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.gradleup.librarian.core.tooling.GH
import com.gradleup.librarian.core.tooling.init.initWriterside
import kotlin.io.path.Path

internal class WriterSideCommand : CliktCommand(help = "Adds an template Writerside documentation to the project") {
  override fun run() {
    Path(".").apply {
      val repository = GH().repository()
      initWriterside(repository)
    }
  }
}
