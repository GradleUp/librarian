#!/usr/bin/env kotlin

@file:DependsOn("com.gradleup.librarian:core:0.0.3")

import com.gradleup.librarian.core.tooling.commitRelease


fun getInput(name: String): String {
  return getOptionalInput(name) ?: error("Cannot find an input for $name")
}

fun getOptionalInput(name: String): String? {
  return System.getenv("INPUT_${name.uppercase()}")?.ifBlank {
    null
  }
}

commitRelease(getInput("versionToRelease"))