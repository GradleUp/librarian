package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.useLines

fun currentYear(): String {
  return Instant.now().atOffset(ZoneOffset.UTC).year.toString()
}

fun String.toSupportedLicense(): SupportedLicense {
  return SupportedLicense.valueOf(this)
}


// See https://spdx.org/licenses/
enum class SupportedLicense(val fullName: String) {
  MIT("MIT License")
}

fun Path.initLicense(license: SupportedLicense, year: String, copyright: String) {
  val variableValues = mapOf("year" to year, "copyright" to copyright)

  readResource("licenses/${license.name}", variableValues)
      .writeTo(resolve("LICENSE"))
}

fun Path.guessLicenseOrNull(): SupportedLicense? {
  Files.list(this).forEach {
    require(!it.name.startsWith("LICENSE.")) {
      "The LICENSE file must not have an extension (found '${it}'). Please rename your LICENSE file"
    }
  }

  resolve("LICENSE").apply {
    if (!exists()) {
      return null
    }
    useLines {
      it.take(5).forEach { line ->
        if (line.contains("MIT License")) {
          return SupportedLicense.MIT
        }
      }
    }
  }

  error("Cannot guess license")
}