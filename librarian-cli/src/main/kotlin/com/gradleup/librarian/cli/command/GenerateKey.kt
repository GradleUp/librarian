package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.gradleup.librarian.cli.promptConfirm
import com.gradleup.librarian.cli.promptInput
import com.gradleup.librarian.cli.promptPassword
import com.gradleup.librarian.core.tooling.armored
import com.gradleup.librarian.core.tooling.secretKeyRing
import kotlin.io.path.Path
import kotlin.io.path.writeText

class GenerateKey: CliktCommand() {
  override fun run() {
    val name = promptInput("Real name")
    val email = promptInput("Email")
    val password = promptPassword("Private key password")

    Path("librarian_private_key.asc").writeText(secretKeyRing(name, email, password).secretKey().armored())

    println("Your private key has been saved as 'librarian_private_key.asc', keep it safe.")

    val upload = promptConfirm("Upload your key to ")
  }
}