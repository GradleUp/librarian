package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.gradleup.librarian.core.tooling.init.initChangelog
import kotlin.io.path.Path

internal class ChangelogCommand: CliktCommand(help = "Adds CHANGELOG.md to the current project") {
  override fun run() {
    with(Path(".")) {
      initChangelog()
    }
  }
}

