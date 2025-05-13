package com.gradleup.librarian.gradle.internal.task

import com.gradleup.librarian.core.tooling.init.SonatypeBackend
import com.gradleup.librarian.core.tooling.init.toBaseUrl

internal fun stagingRepositoryUrl(sonatypeBackend: SonatypeBackend, baseUrl: String?, repoId: String): String {
  check(sonatypeBackend != SonatypeBackend.Portal) {
    "The Central portal does not have a staging url"
  }
  return "${baseUrl ?: sonatypeBackend.toBaseUrl()}/service/local/staging/deployByRepositoryId/${repoId}/"
}

internal fun stagingBaseUrl(sonatypeBackend: SonatypeBackend, baseUrl: String?): String {
  check(sonatypeBackend != SonatypeBackend.Portal) {
    "The Central portal does not have a staging url"
  }
  return baseUrl ?: sonatypeBackend.toBaseUrl()
}

internal fun snapshotsUrl(sonatypeBackend: SonatypeBackend, baseUrl: String?): String {
  if (baseUrl != null) {
    return "$baseUrl/content/repositories/snapshots/"
  }

  return if(sonatypeBackend == SonatypeBackend.Portal) {
    // https://central.sonatype.org/publish/publish-portal-snapshots/#publishing-via-other-methods
    "https://central.sonatype.com/repository/maven-snapshots/"
  } else {
    "${sonatypeBackend.toBaseUrl()}/content/repositories/snapshots/"
  }
}

internal fun deployBaseUrl(baseUrl: String?): String {
  return baseUrl ?: "https://central.sonatype.com"
}

