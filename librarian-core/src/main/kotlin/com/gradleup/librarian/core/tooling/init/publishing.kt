package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.GitHubRepository
import java.nio.file.Path
import kotlin.io.path.writeText

fun Path.initPublishing(
    javaCompatibility: String,
    kotlinCompatibility: String,
    sonatypeBackend: SonatypeBackend,
    groupId: String,
    repository: GitHubRepository,
    license: SupportedLicense,
) {
  val developer = "${repository.name} authors"

  resolve("librarian.properties").writeText(
      """
      java.compatibility=$javaCompatibility
      kotlin.compatibility=$kotlinCompatibility

      kdoc.olderVersions=
      kdoc.artifactId=kdoc

      sonatype.backend=$sonatypeBackend

      pom.groupId=$groupId
      pom.version=0.0.1-SNAPSHOT
      pom.description=${repository.name}
      pom.vcsUrl=https://github.com/${repository.owner}/${repository.name}
      pom.developer=$developer
      pom.license=${license.fullName}
      pom.licenseUrl=https://raw.githubusercontent.com/${repository.name}/${repository.owner}/main/LICENSE
    """.trimIndent()
  )
}

enum class SonatypeBackend {
  Default,
  S01,
  Portal
}

enum class SonatypeRelease {
  Automatic,
  Manual
}