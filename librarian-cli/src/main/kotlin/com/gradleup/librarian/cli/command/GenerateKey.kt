package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptInputPassword
import com.gradleup.librarian.core.tooling.armored
import com.gradleup.librarian.core.tooling.secretKeyRing
import kotlin.io.path.Path
import kotlin.io.path.writeText

class GenerateKey: CliktCommand() {
  override fun run() {
    val name = KInquirer.promptInput("Real name")
    val email = KInquirer.promptInput("Email")
    val password = KInquirer.promptInputPassword("Private key password")

    Path("librarian_private_key.asc").writeText(secretKeyRing(name, email, password).secretKey().armored())

    println("Your private key has been saved as 'librarian_private_key.asc', keep it safe.")

    val upload = KInquirer.promptConfirm("Upload your key to ")
  }
}