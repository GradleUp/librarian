package com.gradleup.librarian.cli

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.gradleup.librarian.core.tooling.SemVer
import com.gradleup.librarian.core.tooling.semVerOrNull
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.height
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaicBlocking
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Spacer
import com.jakewharton.mosaic.ui.Text
import kotlinx.coroutines.CompletableDeferred
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.system.exitProcess

@OptIn(ExperimentalContracts::class)
internal fun checkOrExit(condition: Boolean, message: () -> String) {
  contract {
    returns() implies condition
  }
  if (!condition) {
    exit(message())
  }
}

internal fun exit(message: String): Nothing {
  println(message)
  exitProcess(1)
}
internal fun ctrlC() {
  exitProcess(1)
}

internal fun promptConfirm(message: String, default: Boolean = false): Boolean {
  TODO("Not yet implemented")
}


private fun myRunMosaic(content: @Composable (CompletableDeferred<Unit>) -> Unit) {
  runMosaicBlocking {
    val exitDeferred = remember { CompletableDeferred<Unit>() }
    LaunchedEffect(Unit) {
      exitDeferred.await()
    }

    content(exitDeferred)
  }
}

internal fun promptChoices(message: String, choices: List<String>): String {
  var result: String? = null
  myRunMosaic { exitDeferred ->
    val index = remember { mutableStateOf(0) }
    Column(modifier = Modifier.onKeyEvent {
      when (it.key) {
        "ArrowUp" -> {
          index.value = (index.value - 1).coerceAtLeast(0)
        }

        "ArrowDown" -> {
          index.value = (index.value + 1).coerceAtMost(choices.size - 1)
        }

        "Enter" -> {
          result = choices[index.value]
          exitDeferred.complete(Unit)
        }

        "c" -> {
          if (it.ctrl) {
            ctrlC()
          }
        }
      }
      false
    }) {
      Text(message)
      Spacer(modifier = Modifier.height(1))
      choices.withIndex().forEach { (i, choice) ->
        if (i == index.value) {
          Text("> $choice", color = Color.Cyan)
        } else {
          Text("  $choice")
        }
      }
    }
  }
  return result!!
}

internal fun promptPassword(message: String): String {
  TODO()
}


internal fun promptMultilinePassword(message: String): String {
  TODO()
}

internal fun promptSemVer(): SemVer {
  var version = promptInput("Version").semVerOrNull()
  while (version == null) {
    println("Invalid version")
    version = promptInput("Version").semVerOrNull()
  }
  return version
}

internal fun promptInput(message: String): String {
  var result: String? = null
  myRunMosaic { exitDeferred ->
    val text = remember { mutableStateOf("") }

    Row {

      Text(message)
      Text(
        value = text.value,
        color = Color.Unspecified,
        modifier = Modifier.onKeyEvent { event ->
          when {
            event == KeyEvent("c", ctrl = true) -> {
              ctrlC()
            }

            event.key.toCharArray().singleOrNull() != null -> {
              text.value += event.key
            }

            event == KeyEvent("Backspace") -> {
              if (text.value.isNotEmpty()) {
                text.value = text.value.dropLast(1)
              }
            }

            event == KeyEvent("Enter") -> {
              result = text.value
              exitDeferred.complete(Unit)
            }

            else -> return@onKeyEvent false
          }
          true
        },
      )
    }
  }
  return result!!
}

internal fun promptInput(name: String, defaultValue: String): String {
  val result = promptInput(message = "$name ($defaultValue): ")
  return if (result.isEmpty()) {
    defaultValue
  } else {
    result
  }
}

internal fun promptYesNo(message: String): Boolean {
  val yes = "yes"
  val no = "no"
  return when (promptChoices(message, listOf(yes, no))) {
    "yes" -> true
    else -> false
  }
}