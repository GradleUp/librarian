package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class MainCommand: CliktCommand() {
  init {
    subcommands(ReleaseCommand())
  }
  override fun run() {

  }
}