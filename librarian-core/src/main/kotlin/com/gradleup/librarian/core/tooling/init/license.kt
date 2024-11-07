package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.readTextResource
import com.gradleup.librarian.core.tooling.writeTextTo
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.useLines

fun currentYear(): String {
  return Instant.now().atOffset(ZoneOffset.UTC).year.toString()
}

fun String.toSupportedLicense(): SupportedLicense {
  return SupportedLicense.valueOf(this)
}


// See https://spdx.org/licenses/
enum class SupportedLicense(val spdxIdentifier: String) {
  MIT("MIT")
}

fun Path.initLicense(license: SupportedLicense, year: String, copyright: String) {
  val variableValues = mapOf("year" to year, "copyright" to copyright)

  readTextResource("licenses/${license.name}", variableValues)
      .writeTextTo(resolve("LICENSE"))
}

fun Path.guessLicenseOrNull(): SupportedLicense? {
  useLines {
    it.forEach { line ->
      if (line.contains("MIT License")) {
        return SupportedLicense.MIT
      }
    }
  }

  return null
}

