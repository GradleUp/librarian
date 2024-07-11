package com.gradleup.librarian.core.tooling

import kotlinx.serialization.json.Json

internal val json = Json

fun String.toJsonElement() = json.parseToJsonElement(this)


