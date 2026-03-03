package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.gradleup.librarian.cli.promptInput
import com.gradleup.librarian.core.tooling.init.initMetadata
import kotlin.io.path.Path

internal class GitHubMetadataCommand : CliktCommand(help = "Configures the GitHub metadata") {
  val description by option()
  val url by option()
  val topics by option()

  override fun run() {
    Path(".").apply {
      val description = description ?: promptInput("Description")
      val topics = (topics ?: promptInput("Topics (comma separated)")).split(",").map { it.trim() }

      initMetadata(description, url, topics)
    }
  }
}