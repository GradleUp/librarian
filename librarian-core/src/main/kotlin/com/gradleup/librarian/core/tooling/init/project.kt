package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.VERSION
import com.gradleup.librarian.core.tooling.GitHubRepository
import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createParentDirectories
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
  readResource("project/gradle.properties").writeTo(resolve("gradle.properties"))
  readResource("project/gradle/libs.versions.toml").writeTo(resolve("gradle/libs.versions.toml"))

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

private fun String.urlencode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
