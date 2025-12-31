package com.gradleup.librarian.cli

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.gradleup.librarian.cli.command.PrepareNextVersion
import com.gradleup.librarian.cli.command.SetVersion
import com.gradleup.librarian.cli.command.VersionContext
import com.gradleup.librarian.cli.command.setCurrentVersion
import com.gradleup.librarian.cli.screen.AskVersion
import com.gradleup.librarian.cli.screen.CommitChangesScreen
import com.gradleup.librarian.cli.screen.MainScreen
import com.gradleup.librarian.cli.screen.SetVersionScreen
import com.gradleup.librarian.core.tooling.bump
import com.gradleup.librarian.core.tooling.copy
import com.gradleup.librarian.core.tooling.getCurrentVersion
import com.gradleup.librarian.core.tooling.semVerOrNull
import com.jakewharton.mosaic.runMosaicBlocking
import kotlinx.coroutines.CompletableDeferred


private class MainCommand(val setVersion: VersionContext.() -> Unit, val setVersionInDocs: VersionContext.() -> Unit) : CliktCommand(invokeWithoutSubcommand = true) {
  override fun run() {
    runMosaicBlocking {
      val exit = remember { CompletableDeferred<Unit>() }
      LaunchedEffect(Unit) {
        exit.await()
      }

      val state = remember { mutableStateOf(State.Main) }

      when (state.value) {
        State.Main -> MainScreen(
          onSetVersion = { state.value = State.SetVersion },
          onPrepareNextVersion = { state.value = State.PrepareNextVersion }
        )

        State.SetVersion -> SetVersionScreen(
          setVersion = setVersion,
          onVersionSet = { state.value = State.CommitChanges },
          onOtherVersion = { state.value = State.AskVersion }
        )
        State.AskVersion -> AskVersion(
          onVersionSet = { state.value = State.CommitChanges }
        )
        State.PrepareNextVersion -> {
          val version = getCurrentVersion().semVerOrNull()
          check (version != null) {
            "Cannot parse version '${getCurrentVersion()}'"
          }
          check(version.isSnapshot) {
            "Current version '$version' does not ends with '-SNAPSHOT'. Call set-version to update it."
          }
          val releaseVersion = version.copy(isSnapshot = false)
          VersionContext(releaseVersion.toString()).setVersionInDocs()
          setCurrentVersion(version.bump().toString())
          state.value = State.CommitChanges
        }
        State.CommitChanges -> CommitChangesScreen {
          exit.complete(Unit)
        }
      }
    }
  }
}

private enum class State {
  Main,
  SetVersion,
  AskVersion,
  PrepareNextVersion,
  CommitChanges
}

/**
 * @param setVersion how to set the version. The version is the version of the repo.
 * @param setDocsVersion how to set the docs version. The docs version is lagging behind the repo version and is not a SNAPSHOT.
 */
fun updateRepo(args: Array<String>, setVersion: VersionContext.() -> Unit = {}, setDocsVersion: VersionContext.() -> Unit) {
  MainCommand(setVersion, setDocsVersion).subcommands(SetVersion(setVersion), PrepareNextVersion(setDocsVersion)).main(args)
}

fun main(args: Array<String>) {
  updateRepo(args) {}
}