package com.gradleup.librarian.core.tooling.init

import java.nio.file.Path
import kotlin.io.path.writeText

val rootPropertiesFilename = "librarian.root.properties"
val modulePropertiesFilename = "librarian.module.properties"

fun Path.initLibrarian(
    javaCompatibility: String,
    kotlinCompatibility: String,
    groupId: String,
    projectUrl: String,
    license: SupportedLicense,
    pomDescription: String,
    pomDeveloper: String
) {
  resolve(rootPropertiesFilename).writeText(
      """
      java.compatibility=$javaCompatibility
      kotlin.compatibility=$kotlinCompatibility

      kdoc.olderVersions=

      pom.groupId=$groupId
      pom.version=0.0.0-SNAPSHOT
      pom.description=$pomDescription
      pom.vcsUrl=$projectUrl
      pom.developer=$pomDeveloper
      pom.license=${license.spdxIdentifier}
    """.trimIndent()
  )
}

enum class SonatypeRelease {
  Automatic,
  Manual
}

val snapshotsBrowseUrl = "https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/"
val snapshotsUrl = "https://central.sonatype.com/repository/maven-snapshots/"
val apiBaseUrl = "https://central.sonatype.com"