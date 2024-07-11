package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import com.gradleup.librarian.core.tooling.init.initMetadata
import kotlin.io.path.Path

internal class MetadataCommand : CliktCommand(help = "Configures the GitHub metadata") {
  val description by option()
  val url by option()
  val topics by option()

  override fun run() {
    Path(".").apply {
      val description = description ?: KInquirer.promptInput("Description")
      val topics = (topics ?: KInquirer.promptInput("Topics (comma separated)")).split(",").map { it.trim() }

      initMetadata(description, url, topics)
    }
  }
}