package com.gradleup.librarian.core.tooling.init

import java.nio.file.Path
import kotlin.io.path.writeText

fun Path.initLibrarian(
    javaCompatibility: String,
    kotlinCompatibility: String,
    sonatypeBackend: SonatypeBackend,
    groupId: String,
    projectUrl: String,
    licenseUrl: String,
    license: SupportedLicense,
    pomDescription: String,
    pomDeveloper: String
) {
  resolve("librarian.properties").writeText(
      """
      java.compatibility=$javaCompatibility
      kotlin.compatibility=$kotlinCompatibility

      kdoc.olderVersions=

      sonatype.backend=$sonatypeBackend

      pom.groupId=$groupId
      pom.version=0.0.1-SNAPSHOT
      pom.description=$pomDescription
      pom.vcsUrl=$projectUrl
      pom.developer=$pomDeveloper
      pom.license=${license.displayName}
      pom.licenseUrl=${licenseUrl}
    """.trimIndent()
  )
}

fun SonatypeBackend.toBaseUrl(): String {
  return when (this) {
    SonatypeBackend.S01 -> "https://s01.oss.sonatype.org"
    SonatypeBackend.Default -> "https://oss.sonatype.org"
    SonatypeBackend.Portal -> TODO()
  }
}

enum class SonatypeBackend {
  Portal,
  S01,
  Default,
}

enum class SonatypeRelease {
  Automatic,
  Manual
}