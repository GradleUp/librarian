package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.gradleup.librarian.core.tooling.init.initGitIgnore
import kotlin.io.path.Path

internal class GitIgnoreCommand: CliktCommand(help = "Adds a .gitignore file to the current project") {
  override fun run() {
    Path(".").apply {
      initGitIgnore()
    }
  }
}
