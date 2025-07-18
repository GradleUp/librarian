package com.gradleup.librarian.cli

import com.github.ajalt.mordant.terminal.Terminal
import com.gradleup.librarian.cli.command.checkOrExit
import java.io.File

fun requireInteraction() {
  checkOrExit(Terminal().info.inputInteractive) {
    "This command needs to be run from an interactive terminal"
  }
}