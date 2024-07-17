package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.gradleup.librarian.core.tooling.init.initGitHub
import kotlin.io.path.Path

internal class GitHubCommand : CliktCommand(help = "Configures various aspects of the GitHub repository (write access to the GitHub token, gh-pages setup)") {
  override fun run() {
    Path(".").apply {
      initGitHub()
    }
  }
}
