package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.gradleup.librarian.cli.promptMultilinePassword
import com.gradleup.librarian.cli.requirePassword
import com.gradleup.librarian.core.tooling.GitHubRepository
import com.gradleup.librarian.core.tooling.getAvailableOrganizations
import com.gradleup.librarian.core.tooling.init.Secrets
import com.gradleup.librarian.core.tooling.init.SonatypeBackend
import com.gradleup.librarian.core.tooling.init.SupportedLicense
import com.gradleup.librarian.core.tooling.init.currentYear
import com.gradleup.librarian.core.tooling.init.initActions
import com.gradleup.librarian.core.tooling.init.initChangelog
import com.gradleup.librarian.core.tooling.init.initCodeStyle
import com.gradleup.librarian.core.tooling.init.initGitHub
import com.gradleup.librarian.core.tooling.init.initGitIgnore
import com.gradleup.librarian.core.tooling.init.initGradleWrapper
import com.gradleup.librarian.core.tooling.init.initLicense
import com.gradleup.librarian.core.tooling.init.initMetadata
import com.gradleup.librarian.core.tooling.init.initProject
import com.gradleup.librarian.core.tooling.init.initPublishing
import com.gradleup.librarian.core.tooling.init.initSecrets
import com.gradleup.librarian.core.tooling.init.initWriterside
import com.gradleup.librarian.core.tooling.runCommand
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.system.exitProcess

internal class Create : CliktCommand() {
  private val directory by argument()

  override fun run() {
    with(Path(directory)) {
      val repository = GitHubRepository(
          KInquirer.promptInput(message = "GitHub repository name", name),
          KInquirer.promptList(message = "GitHub repository owner", getAvailableOrganizations())
      )
      val copyright = KInquirer.promptInput("Copyright holder")
      val multiplatform = KInquirer.promptConfirm("Multiplatform")
      val indent = KInquirer.promptInput("Indent size", "4")
      val shortDescription = KInquirer.promptInput("Short description", repository.name)
      val topics = KInquirer.promptInput("Topics", repository.name).split(",").map { it.trim() }
      val groupId = KInquirer.promptInput("Maven group id", "io.github.${repository.owner}.${repository.name}")
      val sonatypeBackend = KInquirer.promptList("Sonatype backend", SonatypeBackend.entries.map { it.name })
      val javaCompatibility = KInquirer.promptInput("Java compatibility", "8")
      val kotlinCompatibility = KInquirer.promptInput("Kotlin compatibility", "2.0.0")

      initActions(if (multiplatform) "macos-latest" else "ubuntu-latest")
      initChangelog()
      initCodeStyle(indent)
      initGitIgnore()
      initGradleWrapper()
      initLicense(SupportedLicense.MIT, currentYear(), copyright)
      initPublishing(javaCompatibility, kotlinCompatibility, SonatypeBackend.valueOf(sonatypeBackend), groupId, repository, SupportedLicense.MIT)
      initWriterside(repository)

      initProject(multiplatform, repository.name, groupId, "module", repository)

      print("Initializing git repository...")

      runCommand("git", "init")
      runCommand("git", "add", ".")
      runCommand("git", "commit", "-a", "-m", "initial commit")

      val upload = KInquirer.promptConfirm(
          "Upload your project to GitHub at ${repository.owner}/${repository.name} and make it public?",
          default = true
      )
      if (!upload) {
        println("run 'librarian init' to finish configuration and set GitHub metadata, settings and secrets")
        exitProcess(0)
      }

      runCommand("gh", "repo", "create", "--public", "-s", ".", "--push")

      initGitHub()
      initMetadata(shortDescription, null, topics)

      println("Paste your armoured GPG key beginning with '-----BEGIN PGP PRIVATE KEY BLOCK-----' (press Enter 3 times when done)")
      val signingPrivateKey = KInquirer.promptMultilinePassword("LIBRARIAN_SIGNING_PRIVATE_KEY")
      val signingPrivateKeyPassword = requirePassword("LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD")
      val sonatypeUsername = requirePassword("LIBRARIAN_SONATYPE_USERNAME")
      val sonatypePassword = requirePassword("LIBRARIAN_SONATYPE_PASSWORD")

      initSecrets(
          Secrets(
              signingPrivateKey = signingPrivateKey,
              signingPrivateKeyPassword = signingPrivateKeyPassword,
              sonatypeUsername = sonatypeUsername,
              sonatypePassword = sonatypePassword
          )
      )
    }
  }
}
