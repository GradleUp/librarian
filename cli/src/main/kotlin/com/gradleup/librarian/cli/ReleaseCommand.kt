package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class ReleaseCommand: CliktCommand(){
  val versionToRelease by option()
      .required()
      .help("The version to release")

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
