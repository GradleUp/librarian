#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.0.11-SNAPSHOT-ba8b5ecfcbda070ecc3b5b95056ee359199552b4")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.gradleup.librarian.cli.command.PrepareNextVersion
import com.gradleup.librarian.cli.command.SetVersion


class MainCommand : CliktCommand() {
    override fun run() {
    }
}

MainCommand().subcommands(SetVersion(), PrepareNextVersion {
    file("README.md") {
        replacePluginId("com.gradleup.librarian")
    }
}).main(args)
