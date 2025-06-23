package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.gradleup.librarian.cli.command.init.ActionsCommand
import com.gradleup.librarian.cli.command.init.ChangelogCommand
import com.gradleup.librarian.cli.command.init.CodeStyleCommand
import com.gradleup.librarian.cli.command.init.GitHubCommand
import com.gradleup.librarian.cli.command.init.GitIgnoreCommand
import com.gradleup.librarian.cli.command.init.GradleWrapperCommand
import com.gradleup.librarian.cli.command.init.LicenseCommand
import com.gradleup.librarian.cli.command.init.GitHubSecretsCommand
import com.gradleup.librarian.cli.command.init.GitHubMetadataCommand
import com.gradleup.librarian.cli.command.init.WriterSideCommand
import com.gradleup.librarian.core.tooling.init.SupportedLicense
import com.gradleup.librarian.core.tooling.init.currentYear
import com.gradleup.librarian.core.tooling.init.guessLicenseOrNull
import com.gradleup.librarian.core.tooling.init.initActions
import com.gradleup.librarian.core.tooling.init.initChangelog
import com.gradleup.librarian.core.tooling.init.initLibrarian
import com.gradleup.librarian.core.tooling.init.initLicense
import com.gradleup.librarian.core.tooling.init.initWriterside
import com.gradleup.librarian.core.tooling.init.kotlinPluginVersion
import com.gradleup.librarian.core.tooling.init.toSupportedLicense
import com.gradleup.librarian.core.tooling.gitHubRepositoryOrNull
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.system.exitProcess


internal class Init : CliktCommand(invokeWithoutSubcommand = true) {
  init {
    subcommands(ActionsCommand(),
        ChangelogCommand(),
        CodeStyleCommand(),
        GitHubCommand(),
        GitIgnoreCommand(),
        GradleWrapperCommand(),
        LicenseCommand(),
        GitHubMetadataCommand(),
        GitHubSecretsCommand(),
        WriterSideCommand()
    )
  }
  override fun run() {
    val subcommand = currentContext.invokedSubcommand
    if (subcommand == null) {
      with(Path(".")) {
        val gitHubRepository = gitHubRepositoryOrNull()

        val licenseCandidates = Files.list(this).filter { it.name.startsWith("LICENSE") }.toListPolyfill()
        val license: SupportedLicense
        if (licenseCandidates.isEmpty()) {
          val defaultCopyrightOwners = "${gitHubRepository?.name ?: name} authors"
          license = KInquirer.promptList("License", SupportedLicense.entries.map { it.name }).toSupportedLicense()
          val copyrightHolder = KInquirer.promptInput("Copyright holder", defaultCopyrightOwners)
          println("Creating LICENSE...")
          initLicense(license, currentYear(), copyrightHolder)
        } else if (licenseCandidates.size == 1) {
          val licensePath = licenseCandidates.single()
          license = licensePath.guessLicenseOrNull() ?: exit( "Cannot guess license from ${licenseCandidates.single()}")
          if (licensePath.name != "LICENSE") {
            println("Moving '${licensePath.name}' to 'LICENSE'...")
            licensePath.moveTo(resolve("LICENSE"))
          }
        } else {
          println("Multiple license files found")
          exitProcess(1)
        }

        val changelogCandidates = Files.list(this).filter { it.name.startsWith("CHANGELOG") }.toListPolyfill()
        if (changelogCandidates.isEmpty()) {
          println("Creating CHANGELOG.md...")
          initChangelog()
        } else if (changelogCandidates.size == 1) {
          val changelogPath = changelogCandidates.single()
          if (changelogPath.name != "CHANGELOG.md") {
            println("Moving '${changelogPath.name}' to 'CHANGELOG.md'...")
            changelogPath.moveTo(resolve("CHANGELOG.md"))
          }
        } else {
          println("Multiple CHANGELOG files found")
          exitProcess(1)
        }

        val defaultDevelopers = "${gitHubRepository?.name ?: name} authors"

        val groupId = KInquirer.promptInput("Maven group id")
        val pomDescription = KInquirer.promptInput("Maven pom description")
        val pomDeveloper = KInquirer.promptInput("Maven pom developer", defaultDevelopers)
        val projectUrl = gitHubRepository?.url() ?: KInquirer.promptInput("Maven pom project url")
        val javaCompatibility = KInquirer.promptInput("Java compatibility", "8")
        val kotlinCompatibility = KInquirer.promptInput("Kotlin compatibility", kotlinPluginVersion)

        initLibrarian(
            javaCompatibility = javaCompatibility,
            kotlinCompatibility = kotlinCompatibility,
            groupId = groupId,
            projectUrl = projectUrl,
            license = license,
            pomDescription = pomDescription,
            pomDeveloper = pomDeveloper
        )

        if (gitHubRepository != null) {
          val addDocumentationSite = KInquirer.promptConfirm("Add Writerside documentation site?", true)
          if (addDocumentationSite) {
            // Writer side "edit on GitHub" requires GitHub
            initWriterside(gitHubRepository)
          }
          initActions("macos-latest", addDocumentationSite)
        }
      }
    }
  }
}

private fun <T> Stream<T>.toListPolyfill(): List<T> = buildList {
  this@toListPolyfill.forEach { add(it) }
}


