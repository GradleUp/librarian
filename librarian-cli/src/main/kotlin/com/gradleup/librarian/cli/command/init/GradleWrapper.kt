package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.gradleup.librarian.core.tooling.init.initGradleWrapper
import kotlin.io.path.Path

class GradleWrapperCommand: CliktCommand(help = "Adds a Gradle wrapper to the current project") {
  override fun run() {
    Path(".").apply {
      initGradleWrapper()
    }
  }
}
