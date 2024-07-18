package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.util.stream.Stream
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.useLines
import kotlin.streams.toList

fun currentYear(): String {
  return Instant.now().atOffset(ZoneOffset.UTC).year.toString()
}

fun String.toSupportedLicense(): SupportedLicense {
  return SupportedLicense.valueOf(this)
}


// See https://spdx.org/licenses/
enum class SupportedLicense(val displayName: String) {
  MIT("MIT License")
}

fun Path.initLicense(license: SupportedLicense, year: String, copyright: String) {
  val variableValues = mapOf("year" to year, "copyright" to copyright)

  readResource("licenses/${license.name}", variableValues)
      .writeTo(resolve("LICENSE"))
}

fun Path.guessLicenseOrNull(): SupportedLicense? {
  useLines {
    it.take(5).forEach { line ->
      if (line.contains("MIT License")) {
        return SupportedLicense.MIT
      }
    }
  }

  return null
}

