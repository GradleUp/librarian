// To access toAnsi()
@file:Suppress(
    "CANNOT_OVERRIDE_INVISIBLE_MEMBER",
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
)

package com.gradleup.librarian.cli

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptInputPassword
import com.github.kinquirer.core.Component
import com.github.kinquirer.core.KInquirerEvent
import com.github.kinquirer.core.KInquirerEvent.Character
import com.github.kinquirer.core.KInquirerEvent.KeyPressBackspace
import com.github.kinquirer.core.KInquirerEvent.KeyPressEnter
import com.github.kinquirer.core.KInquirerEvent.KeyPressSpace
import com.github.kinquirer.core.toAnsi


internal fun requireInput(message: String): String {
  while (true) {
    KInquirer.promptInput(message).let {
      if (it.isNotBlank()) {
        return it
      }
    }
  }
}

internal fun requirePassword(message: String): String {
  while (true) {
    KInquirer.promptInputPassword(message).let {
      if (it.isNotBlank()) {
        return it
      }
    }
  }
}


internal fun KInquirer.promptMultilinePassword(message: String): String {
  return prompt(MultilineSecretComponent(message))
}

class MultilineSecretComponent(private val message: String) : Component<String> {
  private var interacting = true
  private var value: String? = null
  private var consecutiveNewLines = 0

  override fun isInteracting(): Boolean {
    return interacting
  }

  override fun onEvent(event: KInquirerEvent) {
    when (event) {
      is KeyPressEnter -> {
        consecutiveNewLines++
        if (consecutiveNewLines == 3) {
          interacting = false
        } else {
          value = value.orEmpty() + "\n"
        }
      }
      is KeyPressBackspace -> {
        value = value?.dropLast(1)
      }
      is KeyPressSpace -> {
        value = value?.plus(" ") ?: " "
      }
      is Character -> {
        value = value.orEmpty() + event.c
        consecutiveNewLines = 0
      }
      else -> {}
    }
  }

  override fun render(): String = buildString {
    // Question mark character
    append("?".toAnsi { fgGreen(); bold() })
    append(" ")

    // Message
    append(message.toAnsi { bold() })
    append(" ")

    val stars = value().mask()
    when {
      interacting -> {
        // User Input
        append(stars)
      }
      else -> {
        // User Input with new line
        appendLine(stars.toAnsi { fgCyan(); bold(); })
      }
    }
  }

  override fun value(): String {
    return value ?: ""
  }

  private fun String.mask(): String {
    return if (isEmpty()) {
      ""
    } else {
      "(pasted secret)"
    }
  }
}
