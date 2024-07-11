package com.gradleup.librarian.cli.command.init

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.gradleup.librarian.core.tooling.init.SupportedLicense
import com.gradleup.librarian.core.tooling.init.currentYear
import com.gradleup.librarian.core.tooling.init.initLicense
import com.gradleup.librarian.core.tooling.init.toSupportedLicense
import kotlin.io.path.Path


internal class LicenseCommand: CliktCommand(help = "Adds a LICENSE file to the current project") {
  val license by option().enum<SupportedLicense>()
  val copyright by option()

  override fun run() {
    val license = license ?: KInquirer.promptList("License", choices = SupportedLicense.entries.map { it.name }).toSupportedLicense()
    val copyright = copyright ?: KInquirer.promptInput("Copyright holder")
    Path(".").apply {
      initLicense(license, currentYear(), copyright)
    }
  }
}

