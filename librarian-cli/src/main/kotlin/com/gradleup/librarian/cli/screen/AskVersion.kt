package com.gradleup.librarian.cli.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.gradleup.librarian.cli.command.setCurrentVersion
import com.gradleup.librarian.cli.component.ListInput
import com.gradleup.librarian.cli.component.TextInput
import com.gradleup.librarian.core.tooling.getCurrentVersion
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text

@Composable
fun AskVersion(
  onVersionSet: () -> Unit,
) {
  Row {
    val text = remember { mutableStateOf(getCurrentVersion()) }
    Text("What version should be set? ")
    TextInput(
      value = text.value,
      onEnter = {
        setCurrentVersion(text.value)
        onVersionSet()
      },
      onTextChanged = {
        text.value = it
      },
    )
  }
}