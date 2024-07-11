package com.gradleup.librarian.core.tooling

import java.nio.file.Path

fun Path.runCommand(vararg arg: String) {
  ProcessBuilder()
      .command(*arg)
      .inheritIO()
      .directory(this.toFile())
      .start()
      .waitFor()
      .also {
        check(it == 0) {
          "Cannot run '${arg.joinToString(" ")}' ($it)"
        }
      }
}


class ProcessResult(
    private val command: String,
    val code: Int,
    val stdout: String
) {
    fun stdoutOrThrow(): String {
        check(code == 0) {
            "Cannot run '$command' ($code):\n$stdout"
        }

        return stdout
    }
}

fun Path.runCommandAndCaptureStdout(vararg args: String): ProcessResult {
  val builder = ProcessBuilder(*args)
      .directory(toFile())
      .redirectError(ProcessBuilder.Redirect.INHERIT)

  val process = builder.start()
  val ret = process.waitFor()

  val output = process.inputStream.bufferedReader().readText()
  return ProcessResult(args.joinToString(" "), ret, output)
}
