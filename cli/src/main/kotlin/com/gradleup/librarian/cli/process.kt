package com.gradleup.librarian.cli

import java.io.File

internal fun File.runCommand(vararg arg: String) {
  ProcessBuilder()
      .command(*arg)
      .inheritIO()
      .directory(this)
      .start()
      .waitFor()
      .also {
        check(it == 0) {
          "Cannot run '${arg.joinToString(" ")}' ($it)"
        }
      }
}


internal class ProcessResult(
    val code: Int,
    val stdout: String
)

internal fun File.runCommandAndCaptureStdout(vararg args: String): ProcessResult {
  val builder = ProcessBuilder(*args)
      .directory(this)
      .redirectError(ProcessBuilder.Redirect.INHERIT)

  val process = builder.start()
  val ret = process.waitFor()

  val output = process.inputStream.bufferedReader().readText()
  return ProcessResult(ret, output)
}
