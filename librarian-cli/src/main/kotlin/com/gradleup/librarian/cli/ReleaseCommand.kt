package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument

internal class ReleaseCommand: CliktCommand(){
  private val versionToRelease by argument()

  override fun run() {
    val args = arrayOf("gh", "workflow", "run", "prepare-release.yaml", "-f", "versionToRelease=$versionToRelease")
    ProcessBuilder()
        .inheritIO()
        .command(*args)
        .start()
        .waitFor()
        .assertSuccess(args)
  }
}

private fun Int.assertSuccess(args: Array<String>) {
  check(this == 0) {
    "Command '${args.joinToString(" ")}' exited with code: '$this'"
  }
}
