package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

internal class CreateCommand : CliktCommand() {
  private val directory by argument()

  override fun run() {
    with(File(directory)) {
      check(!exists()) {
        "'$directory' already exists"
      }
      val repositoryName = KInquirer.promptInput(message = "GitHub repository name", File(directory).canonicalFile.name)
      val orgs = getAvailableOrganizations()
      val repositoryOwner = KInquirer.promptList(message = "GitHub repository owner", orgs)
      val developer = "$repositoryName authors"

      val groupId = KInquirer.promptInput("Maven group id", "io.github.$repositoryOwner.$repositoryName")
      val sonatypeHost = KInquirer.promptList("Sonatype host", listOf("Default", "S01"))
      val moduleName = KInquirer.promptInput("Module name", "module")

      val javaCompatibility = 8
      val kotlinCompatibility = "2.0.0"

      val license = SupportedLicenses.MIT

      mkdirs()

      file("librarian.properties").writeText("""
      java.compatibility=$javaCompatibility
      kotlin.compatibility=$kotlinCompatibility

      kdoc.olderVersions=
      kdoc.artifactId=kdoc

      sonatype.host=$sonatypeHost

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

      copyResource(license.name, "LICENSE")
      copyResource("gitignore", ".gitignore")
      copyResource("CHANGELOG.md")
      copyResource("gradle.properties")

      copyResource("codeStyles/codeStyleConfig.xml", ".idea/codeStyles/codeStyleConfig.xml")
      copyResource("codeStyles/Project.xml", ".idea/codeStyles/Project.xml")

      copyResource("actions/check-pull-request.yaml", ".github/workflows/check-pull-request.yaml")
      copyResource("actions/prepare-release.yaml", ".github/workflows/prepare-release.yaml")
      copyResource("actions/publish-release.yaml", ".github/workflows/publish-release.yaml")
      copyResource("actions/publish-snapshot.yaml", ".github/workflows/publish-snapshot.yaml")

      /**
       * copied from https://github.com/spring-io/initializr/tree/fbbbe6734e55b4f6393624985c06161b16c9fe8f/initializr-generator-spring/src/main/resources/gradle/8
       * TODO: download latest version automatically
       */
      copyResource("gradlew", "gradlew", true)
      copyResource("gradlew.bat", "gradlew.bat", true)
      copyResource("gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.jar")
      copyResource("gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.properties")

      file("gradle").mkdirs()
      file("gradle/gradle-wrapper.properties").writeText("""
      distributionBase=GRADLE_USER_HOME
      distributionPath=wrapper/dists
      distributionUrl=https\://services.gradle.org/distributions/gradle-${latestGradleRelease()}-bin.zip
      networkTimeout=10000
      validateDistributionUrl=true
      zipStoreBase=GRADLE_USER_HOME
      zipStorePath=wrapper/dists
    """.trimIndent()
      )
      file("gradle/libs.versions.toml").writeText("[libraries]", false)
      file("build.gradle.kts").writeText("""
      import com.gradleup.librarian.core.librarianRoot
      
      plugins {
        id("org.jetbrains.kotlin.jvm").version("${latestKotlinRelease()}").apply(false)
        id("com.gradleup.librarian").version("${latestLibrarianRelease()}").apply(false)
      }
      
      librarianRoot()
    """.trimIndent(),
          overwrite = false
      )

      file(moduleName).let { moduleDir ->
        moduleDir.mkdirs()
        moduleDir.resolve("build.gradle.kts").writeText("""
        import com.gradleup.librarian.core.librarianModule
        
        plugins {
          id("org.jetbrains.kotlin.jvm")
        }
          
        librarianModule()  
        
      """.trimIndent()
        )
        moduleDir.resolve("README.md").writeText("""
          # Module $moduleName
        """.trimIndent()
        )
      }
      file("settings.gradle.kts").writeText("""
        pluginManagement {
          listOf(repositories, dependencyResolutionManagement.repositories).forEach {
            it.apply {
              mavenCentral()
              google()
            }
          }
        }

        include(":$moduleName")
    """.trimIndent(), false
      )

      runCommand("git", "init")
      runCommand("git", "add", ".")
      runCommand("git", "commit", "-a", "-m", "initial commit")

      val upload = KInquirer.promptConfirm("Upload your project to GitHub at $repositoryOwner/$repositoryName and make it public?", default = true)
      if (upload) {
        runCommand("gh", "repo", "create", "--public", "-s", ".", "--push")
        setupGitHub()
      } else {
        println("run 'librarian setup-github' to finish configuration")
      }
    }
  }
}

private fun getAvailableOrganizations(): List<String> {
  return with(File(".")) {
    var result = runCommandAndCaptureStdout("gh", "api", "user", "--jq", ".login")
    check(result.code == 0) {
      "Cannot run 'gh api user', make sure you have 'gh' installed. See https://cli.github.com/."
    }
    val username = result.stdout.trim()
    result = runCommandAndCaptureStdout("gh", "org", "list")
    check(result.code == 0) {
      "Cannot run 'gh org list', make sure you have 'gh' installed. See https://cli.github.com/."
    }

    listOf(username) + result.stdout.lines().filter {
      it.isNotBlank() && !it.startsWith("Showing ")
    }
  }
}

private fun File.writeText(text: String, overwrite: Boolean) {
  if (exists() && !overwrite) {
    return
  }
  writeText(text)
}

private fun latestLibrarianRelease(): String = latestTag("GradleUp", "librarian").substring(1) // drop the v
private fun latestKotlinRelease(): String = latestTag("JetBrains", "kotlin").substring(1) // drop the v
private fun latestGradleRelease(): String = latestTag("gradle", "gradle").toGradleRelease()

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

private fun File.copyResource(resourceName: String) {
  copyResource(resourceName, resourceName)
}

private fun File.file(path: String): File = resolve(path)

private fun File.copyResource(resourceName: String, destinationPath: String, makeExecutable: Boolean = false) {
  val stream = CreateCommand::class.java.classLoader.getResourceAsStream(resourceName)
  require(stream != null) {
    "Cannot open resource '$resourceName'"
  }
  stream.buffered().use { inputStream ->
    file(destinationPath).let {
      it.parentFile?.mkdirs()
      it.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
      }
    }
  }
  if (makeExecutable) {
    Files.setPosixFilePermissions(
        file(destinationPath).toPath(),
        PosixFilePermissions.fromString("rwxr-xr-x")
    )
  }
}

// See https://spdx.org/licenses/
enum class SupportedLicenses(val fullName: String) {
  MIT("MIT License")
}
