package com.gradleup.librarian.cli

import kotlinx.serialization.json.Json

internal val json = Json

fun String.toJsonElement() = json.parseToJsonElement(this)


