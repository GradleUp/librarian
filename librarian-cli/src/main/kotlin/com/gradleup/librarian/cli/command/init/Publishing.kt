package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.gradleup.librarian.core.tooling.GH
import com.gradleup.librarian.core.tooling.init.SonatypeBackend
import com.gradleup.librarian.core.tooling.init.guessLicenseOrNull
import com.gradleup.librarian.core.tooling.init.initPublishing
import kotlin.io.path.Path

class PublishingCommand: CliktCommand(help = "Adds librarian.properties to the current project") {
  override fun run() {
    with(Path(".")) {
      val repository = GH().repository()
      val license = guessLicenseOrNull() ?: error("No LICENSE found")
      val groupId = KInquirer.promptInput("Maven group id", "io.github.${repository.owner}.${repository.name}")
      val sonatypeBackend = KInquirer.promptList("Sonatype backend", SonatypeBackend.entries.map { it.name })
      val javaCompatibility = KInquirer.promptInput("Java compatibility","8")
      val kotlinCompatibility = KInquirer.promptInput("Kotlin compatibility", "2.0.0")

      initPublishing(javaCompatibility, kotlinCompatibility, SonatypeBackend.valueOf(sonatypeBackend), groupId, repository, license)
    }
  }
}