package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.VERSION
import com.gradleup.librarian.core.tooling.GitHubRepository
import com.gradleup.librarian.core.tooling.readTextResource
import com.gradleup.librarian.core.tooling.writeTextTo
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

val kotlinPluginVersion = "2.0.0"

fun Path.initProject(
    multiplatform: Boolean,
    projectTitle: String,
    readmeDescription: String,
    groupId: String,
    artifactId: String,
    repository: GitHubRepository,
    addDocumentationSite: Boolean,
    sonatypeBackend: SonatypeBackend,
) {
  readTextResource("project/gradle.properties").writeTextTo(resolve("gradle.properties"))
  readTextResource("project/gradle/libs.versions.toml").writeTextTo(resolve("gradle/libs.versions.toml"))
  readTextResource("project/settings.gradle.kts").writeTextTo(resolve("settings.gradle.kts"))

  val kotlinPluginId = if (multiplatform) "org.jetbrains.kotlin.multiplatform" else "org.jetbrains.kotlin.jvm"

  resolve("build.gradle.kts").writeText("""
    import com.gradleup.librarian.gradle.librarianRoot

    plugins {
      id("$kotlinPluginId").version("$kotlinPluginVersion").apply(false)
      id("com.gradleup.librarian").version("$VERSION").apply(false)
    }

    librarianRoot()
  """.trimIndent()
  )

  resolve("module").createDirectory()
  resolve("module/build.gradle.kts").writeText("""
    import com.gradleup.librarian.gradle.librarianModule

    plugins {
      id("org.jetbrains.kotlin.jvm")
    }

    librarianModule()    
  """.trimIndent()
  )

  resolve("README.md").writeText(buildString {
    appendLine("[![Maven Central](https://img.shields.io/maven-central/v/$groupId/$artifactId?style=flat-square)](https://central.sonatype.com/namespace/$groupId)")
    if (sonatypeBackend in setOf(SonatypeBackend.S01, SonatypeBackend.Default)) {
      appendLine("[![OSS Snapshots](https://img.shields.io/nexus/s/$groupId/$artifactId?server=${
        sonatypeBackend.toBaseUrl().urlencode()
      }&label=oss-snapshots&style=flat-square)](${sonatypeBackend.toBaseUrl()}/content/repositories/snapshots/${groupId.replace('.', '/')}/)"
      )
    }

    appendLine()
    appendLine("## $projectTitle")
    appendLine(readmeDescription)

    appendLine("## \uD83D\uDCDA Documentation")

    if (addDocumentationSite) {
      appendLine("See the project website for documentation:<br/>")
      appendLine("[https://${repository.owner}.github.io/${repository.name}/](https://${repository.owner}.github.io/${repository.name}/)")
    }

    appendLine("The Kdoc API reference can be found at: <br/>")
    appendLine("[https://${repository.owner}.github.io/${repository.name}/kdoc](https://${repository.owner}.github.io/${repository.name}/kdoc)")
  })
}

fun ByteArray.writeBinaryTo(destination: Path) {
  destination.writeBytes(this)
}

private fun String.urlencode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
