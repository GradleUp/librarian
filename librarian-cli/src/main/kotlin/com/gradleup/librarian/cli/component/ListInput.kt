package com.gradleup.librarian.cli.component

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle

class Choice<out D>(val text: String, val data: D)

@Composable
fun <D> ListInput(
  question: String,
  choices: List<Choice<D>>,
  index: Int,
  onHighlight: (Int) -> Unit,
  onChoice: (Choice<D>) -> Unit
) {
  var index = index
  Column(modifier = Modifier.onKeyEvent { event ->
    when (event.key) {
      "ArrowUp" -> index -= 1
      "ArrowDown" -> index += 1
      "Enter" -> onChoice(choices.get(index))
      else -> return@onKeyEvent false
    }

    index = index.coerceIn(0, choices.lastIndex)

    onHighlight(index)
    true
  }) {
    Text(question)
    Text("")
    choices.forEachIndexed { i, choice ->
      Text(
        "${if (index == i) ">" else " "} ${choice.text}",
        textStyle = if (index == i) TextStyle.Bold else TextStyle.Unspecified
      )
    }
  }
}

@Composable
@JvmName("ListInput2")
fun ListInput(
  question: String,
  choices: List<String>,
  index: Int,
  onHighlight: (Int) -> Unit,
  onChoice: (Int) -> Unit
) {
  ListInput(
    question = question,
    choices = choices.mapIndexed { index, string -> Choice(string, index) },
    index = index,
    onHighlight = onHighlight,
    onChoice = { onChoice(it.data) }
  )
}