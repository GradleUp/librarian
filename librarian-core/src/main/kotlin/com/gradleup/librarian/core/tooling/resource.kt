package com.gradleup.librarian.core.tooling

import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createParentDirectories
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText

fun readResource(resourceName: String): String {
  return GH::class.java.classLoader.getResourceAsStream(resourceName)?.reader()?.buffered()?.readText() ?: error("No resource found for $resourceName")
}

internal fun readResource(resourceName: String, values: Map<String, String>): String {
  return readResource(resourceName).substituteVariables(values)
}

fun String.writeTo(path: Path) {
  path.createParentDirectories()
  path.writeText(this)
}

fun String.substituteVariables(value: (String) -> String): String {
  return replace(Regex("([^\$])\\{\\{([^}]*)}}")) {
    "${it.groupValues[1]}${value(it.groupValues[2])}"
  }
}

fun String.substituteVariables(values: Map<String, String>): String {
  return substituteVariables { values[it] ?: error("No value found for variable '$it'") }
}

fun Path.makeExecutable() = setPosixFilePermissions(PosixFilePermissions.fromString("rwxr-xr-x"))
