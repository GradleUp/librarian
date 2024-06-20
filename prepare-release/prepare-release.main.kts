#!/usr/bin/env kotlin

@file:DependsOn("com.gradleup.librarian:core:0.0.3")

import com.gradleup.librarian.core.tooling.commitRelease


fun getInput(name: String): String {
  return getOptionalInput(name) ?: error("Cannot find an input for $name")
}

fun getOptionalInput(name: String): String? {
  return System.getenv("INPUT_${name.uppercase()}")?.ifBlank {
    null
  }
}

runCommand("git", "config", "user.name", "librarian[bot]")
runCommand("git", "config", "user.email", "librarian[bot]@users.noreply.github.com")

commitRelease(getInput("versionToRelease"))

private fun runCommand(vararg args: String) {
  ProcessBuilder()
      .inheritIO()
      .command(*args)
      .start()
      .waitFor()
      .let {
        check(it == 0) {
          "Cannot execute '${args.joinToString(" ")}': $it"
        }
      }
}
