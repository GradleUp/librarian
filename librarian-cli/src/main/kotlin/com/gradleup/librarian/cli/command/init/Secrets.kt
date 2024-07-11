package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.github.kinquirer.KInquirer
import com.gradleup.librarian.cli.promptMultilinePassword
import com.gradleup.librarian.cli.requirePassword
import com.gradleup.librarian.core.tooling.init.Secrets
import com.gradleup.librarian.core.tooling.init.initSecrets
import kotlin.io.path.Path

internal class SecretsCommand : CliktCommand(help = "Configures the GitHub secrets") {
  override fun run() {
    Path(".").apply {
      println("Paste your armoured GPG key beginning with '-----BEGIN PGP PRIVATE KEY BLOCK-----' (press Enter 3 times when done)")
      val signingPrivateKey = KInquirer.promptMultilinePassword("LIBRARIAN_SIGNING_PRIVATE_KEY")
      val signingPrivateKeyPassword = requirePassword("LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD")
      val sonatypeUsername = requirePassword("LIBRARIAN_SONATYPE_USERNAME")
      val sonatypePassword = requirePassword("LIBRARIAN_SONATYPE_PASSWORD")

      initSecrets(Secrets(
          signingPrivateKey = signingPrivateKey,
          signingPrivateKeyPassword = signingPrivateKeyPassword,
          sonatypeUsername = sonatypeUsername,
          sonatypePassword = sonatypePassword
      )
      )
    }
  }
}
