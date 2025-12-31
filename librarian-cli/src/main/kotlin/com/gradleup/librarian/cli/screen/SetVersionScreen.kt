package com.gradleup.librarian.cli.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.gradleup.librarian.cli.command.VersionContext
import com.gradleup.librarian.cli.command.getCurrentVersion
import com.gradleup.librarian.cli.command.setCurrentVersion
import com.gradleup.librarian.cli.component.Choice
import com.gradleup.librarian.cli.component.ListInput
import com.gradleup.librarian.core.tooling.nextMajor
import com.gradleup.librarian.core.tooling.nextMinor
import com.gradleup.librarian.core.tooling.nextPatch
import com.gradleup.librarian.core.tooling.nextPrerelease
import com.gradleup.librarian.core.tooling.semVerOrNull

@Composable
fun SetVersionScreen(
  onVersionSet: () -> Unit,
  onOtherVersion: () -> Unit,
  setVersion: VersionContext.() -> Unit,
) {
  val indexState = remember { mutableStateOf(0) }

  val choices = remember {
    val currentVersion = getCurrentVersion().semVerOrNull()
    check(currentVersion != null) {
      "Cannot parse version '$currentVersion'"
    }
    check(currentVersion.isSnapshot) {
      "Current version '$currentVersion' does not ends with '-SNAPSHOT'. Call set-version to update it."
    }

    buildList {
      if (currentVersion.preRelease != null) {
        add(Choice("Bump prerelease (${currentVersion.nextPrerelease()}", currentVersion.nextPrerelease()))
      }
      add(Choice("Bump patch to ${currentVersion.nextPatch()}", currentVersion.nextPatch()))
      add(Choice("Bump minor to ${currentVersion.nextMinor()}", currentVersion.nextMinor()))
      add(Choice("Bump major to ${currentVersion.nextMajor()}", currentVersion.nextMajor()))
      add(Choice("Other?", null))
    }
  }
  ListInput(
    question = "Version is currently '${getCurrentVersion()}', what do you want to set the version to?",
    choices = choices,
    index = indexState.value,
    onHighlight = { indexState.value = it },
    onChoice = {
      when (it.data) {
        null -> onOtherVersion()
        else -> {
          setCurrentVersion(it.data.toString())
          VersionContext(it.data.toString()).setVersion()
          onVersionSet()
        }
      }
    },
  )
}