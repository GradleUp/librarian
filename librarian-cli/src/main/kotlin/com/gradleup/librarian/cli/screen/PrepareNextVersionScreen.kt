package com.gradleup.librarian.cli.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.gradleup.librarian.cli.command.getCurrentVersion
import com.gradleup.librarian.cli.component.ListInput
import com.gradleup.librarian.core.tooling.runCommand

@Composable
fun CommitChangesScreen(
  onDone: () -> Unit,
) {

  val indexState = remember { mutableStateOf(0) }
  ListInput(
    question = "Commit changes?",
    choices = listOf("yes", "no"),
    index = indexState.value,
    onHighlight = { indexState.value = it},
    onChoice = {
      if (it == 0) {
        val currentVersion = getCurrentVersion()
        runCommand("git", "checkout", "-b", "bump-to-$currentVersion")
        runCommand("git", "commit", "-a", "-m", "Bump version to $currentVersion")
      }

      onDone()
    },
  )
}