package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.GH
import java.nio.file.Path

class Secrets(
    val signingPrivateKey: String,
    val signingPrivateKeyPassword: String,
    val sonatypeUsername: String,
    val sonatypePassword: String,
)

fun Path.initSecrets(secrets: Secrets) {
  val gh = GH()

  println("setting secrets...")
  gh.setSecret("LIBRARIAN_SIGNING_PRIVATE_KEY", secrets.signingPrivateKey)
  gh.setSecret("LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD", secrets.signingPrivateKeyPassword)
  gh.setSecret("LIBRARIAN_SONATYPE_USERNAME", secrets.sonatypeUsername)
  gh.setSecret("LIBRARIAN_SONATYPE_PASSWORD", secrets.sonatypePassword)
}
