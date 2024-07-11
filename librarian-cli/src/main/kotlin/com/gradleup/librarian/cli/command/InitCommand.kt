package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.gradleup.librarian.cli.command.init.ActionsCommand
import com.gradleup.librarian.cli.command.init.ChangelogCommand
import com.gradleup.librarian.cli.command.init.CodeStyleCommand
import com.gradleup.librarian.cli.command.init.GitHubCommand
import com.gradleup.librarian.cli.command.init.GitIgnoreCommand
import com.gradleup.librarian.cli.command.init.GradleWrapperCommand
import com.gradleup.librarian.cli.command.init.LicenseCommand
import com.gradleup.librarian.cli.command.init.PublishingCommand
import com.gradleup.librarian.cli.command.init.SecretsCommand
import com.gradleup.librarian.cli.command.init.MetadataCommand
import com.gradleup.librarian.cli.command.init.WriterSideCommand


internal class InitCommand : CliktCommand() {
  init {
    subcommands(ActionsCommand(),
        ChangelogCommand(),
        CodeStyleCommand(),
        GitHubCommand(),
        GitIgnoreCommand(),
        GradleWrapperCommand(),
        LicenseCommand(),
        MetadataCommand(),
        PublishingCommand(),
        SecretsCommand(),
        WriterSideCommand()
    )
  }
  override fun run() {
  }
}



