package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument

internal class SetVersion(val setVersion: VersionContext.() -> Unit) : CliktCommand() {
  val version by argument()
  override fun run() {
    setCurrentVersion(version)

    VersionContext(version).setVersion()

    println("Version is now '$version'.")
  }
}

