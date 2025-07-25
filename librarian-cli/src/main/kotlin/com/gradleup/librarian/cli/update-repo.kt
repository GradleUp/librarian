package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.gradleup.librarian.cli.command.PrepareNextVersion
import com.gradleup.librarian.cli.command.SetVersion
import com.gradleup.librarian.cli.command.VersionContext


private class MainCommand : CliktCommand() {
    override fun run() {
    }
}

fun updateRepo(args: Array<String>, setVersionInDocs: VersionContext.() -> Unit) {
    MainCommand().subcommands(SetVersion(), PrepareNextVersion(setVersionInDocs)).main(args)
}
