package com.gradleup.librarian.gradle.internal.task

import com.gradleup.librarian.core.tooling.init.SonatypeBackend
import com.gradleup.librarian.gradle.toBaseUrl

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
  check(sonatypeBackend != SonatypeBackend.Portal) {
    "The Central portal does not support snapshots"
  }
  return "${baseUrl ?: sonatypeBackend.toBaseUrl()}/content/repositories/snapshots/"
}

internal fun deployBaseUrl(baseUrl: String?): String {
  return baseUrl ?: "https://central.sonatype.com"
}

