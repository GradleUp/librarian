package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.gradleup.librarian.gradle.SonatypeBackend
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

internal fun File.addLibrarian(
  javaCompatibility: Int,
  kotlinCompatibility: String,
  sonatypeBackend: SonatypeBackend,
  groupId: String,
  repositoryOwner: String,
  repositoryName: String,
  license: SupportedLicense,
) {
  val developer = "$repositoryName authors"

  println("✅ librarian.properties")

  resolve("librarian.properties").writeText(
      """
      java.compatibility=$javaCompatibility
      kotlin.compatibility=$kotlinCompatibility

      kdoc.olderVersions=
      kdoc.artifactId=kdoc

      sonatype.backend=$sonatypeBackend

      git.snapshots=main

      pom.groupId=$groupId
      pom.version=0.0.1-SNAPSHOT
      pom.description=$repositoryName
      pom.vcsUrl=https://github.com/$repositoryOwner/$repositoryName
      pom.developer=$developer
      pom.license=${license.fullName}
      pom.licenseUrl=https://raw.githubusercontent.com/$repositoryOwner/$repositoryName/main/LICENSE
    """.trimIndent()
  )
}

internal val javaCompatibility = 8
internal val kotlinCompatibility = "2.0.0"
internal val defaultLicense = SupportedLicense.MIT

internal class GitHubRepository(val owner: String, val name: String)

internal fun requireGh() {
  try {
    File(".").runCommandAndCaptureStdout("gh", "--version")
  } catch (e: IOException) {
    throw Exception("Cannot run 'gh --version'. Is it installed? See https://cli.github.com/ for more details.", e)
  }
}

internal fun File.gitHubRepositoryOrNull(): GitHubRepository? {
  return runCommandAndCaptureStdout("gh", "repo", "view", "--json", "owner,name")
      .let {
        if (it.code != 0) {
          /*
           * Assume it's because we have no GitHub remote in this repository
           * XXX: fine tune error handling
           */
          return null
        }
        it.stdout
      }
      .toJsonElement()
      .run {
        GitHubRepository(
            owner = resolvePathAsStringOrNull("$.owner.login") ?: error("No repository owner found in '${this}'"),
            name = resolvePathAsStringOrNull("$.name") ?: error("No repository name found in '${this}'"),
        )
      }
}

internal fun File.promptGitHubRepository(): GitHubRepository {
  return GitHubRepository(
      KInquirer.promptInput(message = "GitHub repository name", canonicalFile.name),
      KInquirer.promptList(message = "GitHub repository owner", getAvailableOrganizations())
  )
}

internal fun File.init(repository: GitHubRepository, license: SupportedLicense?) {
  var license2 = license
  var addLicense = false
  if (license2 == null) {
    val r = KInquirer.promptConfirm("No LICENSE found. Add a ${defaultLicense.name} license?", default = true)
    check(r) {
      "librarian requires a LICENSE"
    }
    license2 = defaultLicense
    addLicense = true
  }

  require(resolve("librarian.properties").exists().not()) {
    "This project already contains a librarian.properties file. Not overwriting."
  }

  val groupId = KInquirer.promptInput("Maven group id", "io.github.${repository.owner}.${repository.name}")
  val sonatypeBackend = KInquirer.promptList("Sonatype backend", SonatypeBackend.entries.map { it.name })

  println("Adding files:")
  addLibrarian(javaCompatibility, kotlinCompatibility, SonatypeBackend.valueOf(sonatypeBackend), groupId, repository.owner, repository.name, license2)
  copyResource("CHANGELOG.md", "CHANGELOG.md")
  if (addLicense) {
    addLicense(license2.name, repository.name)
  }
}

internal fun File.addLicense(name: String, projectName: String) {
  CreateCommand::class.java.classLoader.getResourceAsStream(name)!!
      .reader()
      .buffered()
      .readText()
      .replace("__COPYRIGHT_LINE__", "Copyright (c) $projectName authors")
      .let {
        resolve("LICENSE").writeText(it)
      }
}

internal class InitCommand : CliktCommand() {
  override fun run() {
    requireGh()

    with(File(".")) {
      init(gitHubRepositoryOrNull() ?: promptGitHubRepository(), guessLicense())
    }
  }
}

private fun getAvailableOrganizations(): List<String> {
  return with(File(".")) {

    val username = runCommandAndCaptureStdout("gh", "api", "user", "--jq", ".login").stdoutOrThrow().trim()
    val organisations = runCommandAndCaptureStdout("gh", "org", "list").stdoutOrThrow().lines().filter {
      it.isNotBlank() && !it.startsWith("Showing ")
    }
    listOf(username) + organisations
  }
}

internal fun File.writeText(text: String, overwrite: Boolean) {
  if (exists() && !overwrite) {
    return
  }
  writeText(text)
}

internal fun latestLibrarianRelease(): String = latestTag("GradleUp", "librarian").substring(1) // drop the v
internal fun latestKotlinRelease(): String = latestTag("JetBrains", "kotlin").substring(1) // drop the v

private fun latestTag(owner: String, name: String): String {
  return Request.Builder()
      .get()
      .url("https://api.github.com/repos/$owner/$name/releases")
      .build()
      .let {
        OkHttpClient().newCall(it).execute()
      }.let {
        check(it.isSuccessful) {
          "Cannot get ${it.request.url} (${it.code}): ${it.body?.string()}"
        }
        Json.parseToJsonElement(it.body!!.string())
      }.let {
        it.resolvePathAsStringOrNull("$[0].tag_name") ?: "Cannot locate tag_name in response: $it"
      }
}

private fun String.toGradleRelease(): String {
  // tag names look like v8.7.0-RC3
  return Regex("v([0-9]*.[0-9]*).[0-9]*(.*)").matchEntire(this).let {
    check(it != null) {
      "Unexpected tag: '$this"
    }
    "${it.groupValues.get(1)}${it.groupValues.get(2).lowercase()}"
  }
}


internal fun File.copyResource(
    resourceName: String,
    destinationPath: String,
    makeExecutable: Boolean = false,
    overwrite: Boolean = false,
) {
  val destination = resolve(destinationPath)
  if (!overwrite && destination.exists()) {
    return
  }

  println("✅ ${destination.name}")

  val stream = CreateCommand::class.java.classLoader.getResourceAsStream(resourceName)
  require(stream != null) {
    "Cannot open resource '$resourceName'"
  }
  stream.buffered().use { inputStream ->
    destination.let {
      it.parentFile?.mkdirs()
      it.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
      }
    }
  }
  if (makeExecutable) {
    Files.setPosixFilePermissions(
        resolve(destinationPath).toPath(),
        PosixFilePermissions.fromString("rwxr-xr-x")
    )
  }
}

// See https://spdx.org/licenses/
internal enum class SupportedLicense(val fullName: String) {
  MIT("MIT License")
}
