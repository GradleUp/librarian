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

val librarianKotlinPluginVersion = "2.0.21"

fun Path.initProject(
    multiplatform: Boolean,
    projectTitle: String,
    readmeDescription: String,
    groupId: String,
    artifactId: String,
    repository: GitHubRepository,
    addDocumentationSite: Boolean,
) {
  readTextResource("project/gradle.properties").writeTextTo(resolve("gradle.properties"))
  readTextResource("project/settings.gradle.kts").writeTextTo(resolve("settings.gradle.kts"))


  resolve("gradle/libs.versions.toml").writeText("""
    [libraries]

    [plugins]
    kgp = { id = "org.jetbrains.kotlin.jvm", version = "$librarianKotlinPluginVersion" }
    librarian = { id = "com.gradleup.librarian", version = "$VERSION" }    
  """.trimIndent())

  resolve("build.gradle.kts").writeText("""
    import com.gradleup.librarian.gradle.Librarian

    plugins {
      alias(libs.plugins.kgp).apply(false)
      alias(libs.plugins.librarian).apply(false)
    }

    Librarian.root(project)
  """.trimIndent()
  )

  val kotlinPluginId = if (multiplatform) "org.jetbrains.kotlin.multiplatform" else "org.jetbrains.kotlin.jvm"

  resolve("module").createDirectory()
  resolve("module/build.gradle.kts").writeText(
      buildString {
        append("""
          import com.gradleup.librarian.gradle.Librarian
      
          plugins {
            id("$kotlinPluginId")
          }
      
          Librarian.module(project)    
        """.trimIndent())

        if (multiplatform) {
          append("""
            kotlin {
              jvm()
            }
          """.trimIndent())
        }
      }
  )

  resolve("README.md").writeText(buildString {
    appendLine("[![Maven Central](https://img.shields.io/maven-central/v/$groupId/$artifactId?style=flat-square)](https://central.sonatype.com/namespace/$groupId)")
    appendLine("[![OSS Snapshots](https://img.shields.io/nexus/s/$groupId/$artifactId?server=${snapshotsUrl.urlencode()}&label=oss-snapshots&style=flat-square)](${snapshotsUrl}/content/repositories/snapshots/${groupId.replace('.', '/')}/)")

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
