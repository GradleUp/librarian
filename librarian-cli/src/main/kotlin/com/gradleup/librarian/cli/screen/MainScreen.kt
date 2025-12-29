package com.gradleup.librarian.cli.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.gradleup.librarian.cli.component.ListInput

@Composable
fun MainScreen(
  onSetVersion: () -> Unit,
  onPrepareNextVersion: () -> Unit,
) {
  val indexState = remember { mutableStateOf(0) }

  ListInput(
    question = "What do you want to do?",
    choices = listOf(
      "Set version.",
      "Prepare next version."
    ),
    index = indexState.value,
    onHighlight = { indexState.value = it },
    onChoice = {
      when (it) {
        0 -> onSetVersion()
        1 -> onPrepareNextVersion()
      }
    },
  )
}