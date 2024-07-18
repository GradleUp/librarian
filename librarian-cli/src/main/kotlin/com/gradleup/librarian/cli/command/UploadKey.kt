package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.gradleup.librarian.core.tooling.armored
import com.gradleup.librarian.core.tooling.secretKeyRingOrNull
import kotlin.io.path.Path

class UploadKey: CliktCommand() {
  val keyFile by argument()

  override fun run() {
    //val password = KInquirer.promptInputPassword("Private key password")

    val secretKeyRing = secretKeyRingOrNull(Path(keyFile))
    checkOrExit (secretKeyRing != null) {
      "Cannot read secret key from $keyFile"
    }

    println(secretKeyRing.publicKey().armored())
    //uploadKey("https://keys.openpgp.org", secretKeyRing.publicKey())
  }
}
