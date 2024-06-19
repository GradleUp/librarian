package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.github.kinquirer.components.promptListObject
import com.github.kinquirer.core.Choice
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class InitCommand : CliktCommand() {
  override fun run() {
    val currentDirectory = File(".").canonicalFile.absoluteFile.let {
      it.relativeTo(it.parentFile).name
    }
    val repositoryName = KInquirer.promptInput(message = "GitHub repository name", currentDirectory)
    val repositoryOwner = requiredInput(message = "GitHub repository owner")
    val developer = KInquirer.promptInput(message = "Developer", "$repositoryName authors")

    val groupId = requiredInput("Maven group id")
    val sonatypeHost = KInquirer.promptList("Sonatype host", listOf("Default", "S01"))

    var javaCompatibility: Int? = null
    while (javaCompatibility == null) {
      javaCompatibility = KInquirer.promptInput("Java compatibility", "8").toIntOrNull()
    }
    var kotlinCompatibility: String? = null
    while (kotlinCompatibility == null) {
      kotlinCompatibility = KInquirer.promptInput("Kotlin compatibility", "2.0.0").toKotlinCompatibility()
    }

    val license = KInquirer.promptListObject("License", SupportedLicenses.entries.map { Choice(it.fullName, it) })

    val indentSize = KInquirer.promptList("Indent size", listOf("2", "4"))

    val moduleName = KInquirer.promptInput("Module name", "module")

    File("librarian.properties").writeText("""
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

    copyResource("codeStyles/codeStyleConfig.xml", ".idea/codeStyles/codeStyleConfig.xml")
    copyResource("codeStyles/Project.xml", ".idea/codeStyles/Project.xml")

    copyResource("actions/check-pull-request.yaml", ".github/workflows/check-pull-request.yaml")
    copyResource("actions/prepare-release.yaml", ".github/workflows/prepare-release.yaml")
    copyResource("actions/publish-release.yaml", ".github/workflows/publish-release.yaml")
    copyResource("actions/publish-snapshot.yaml", ".github/workflows/publish-snapshot.yaml")

    File("gradle").mkdirs()
    File("gradle/gradle-wrapper.properties").writeText("""
      distributionBase=GRADLE_USER_HOME
      distributionPath=wrapper/dists
      distributionUrl=https\://services.gradle.org/distributions/gradle-${latestGradleRelease()}-bin.zip
      networkTimeout=10000
      validateDistributionUrl=true
      zipStoreBase=GRADLE_USER_HOME
      zipStorePath=wrapper/dists
    """.trimIndent())
    File("gradle/libs.versions.toml").writeText("[libraries]", false)
    File("build.gradle.kts").writeText("""
      import com.gradleup.librarian.core.librarianRoot
      
      plugins {
        id("org.jetbrains.kotlin.jvm").version("${latestKotlinRelease()}").apply(false)
        id("com.gradleup.librarian").version("${latestLibrarianRelease()}").apply(false)
      }
      
      librarianRoot()
    """.trimIndent(),
        overwrite = false
    )

    File(moduleName).let { moduleDir ->
      moduleDir.mkdirs()
      moduleDir.resolve("build.gradle.kts").writeText("""
        import com.gradleup.librarian.core.librarianModule
        
        plugins {
          id("org.jetbrains.kotlin.jvm")
        }
          
        librarianModule()  
        
      """.trimIndent())
    }
    File("settings.gradle.kts").writeText("""
        pluginManagement {
          listOf(repositories, dependencyResolutionManagement.repositories).forEach {
            it.apply {
              mavenCentral()
              google()
            }
          }
        }

        include(":module")
    """.trimIndent(), false
    )

    runCommand("git", "init")
    runCommand("git", "add", ".")
    runCommand("git", "commit", "-a", "-m", "initial commit")
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

private fun runCommand(vararg arg: String) {
  ProcessBuilder()
      .command(*arg)
      .inheritIO()
      .start()
      .waitFor()
      .also {
        check(it == 0) {
          "Cannot run '${arg.joinToString(" ")}' ($it)"
        }
      }
}

private fun resourceText(resourceName: String): String {
  return InitCommand::class.java.classLoader.getResource(resourceName)!!.openStream().bufferedReader().readText()
}

private fun copyResource(resourceName: String) {
  copyResource(resourceName, resourceName)
}

private fun copyResource(resourceName: String, destinationPath: String) {
  val stream = InitCommand::class.java.classLoader.getResourceAsStream(resourceName)
  require(stream != null) {
    "Cannot open resource '$resourceName'"
  }
  stream.buffered().use { inputStream ->
    File(destinationPath).outputStream().use { outputStream ->
      inputStream.copyTo(outputStream)
    }
  }
}

private fun String.toKotlinCompatibility(): String? {
  if (Regex("[0-9]*.[0-9]*.[0-9]*").matches(this)) {
    return this
  }

  return null
}
//
//fun KInquirer.promptInput(message: String, default: String): String {
//  return KInquirer.promptInput("$message ($default)").let {
//    if (it.isBlank()) {
//      default
//    } else {
//      it
//    }
//  }
//}


fun requiredInput(message: String): String {
  while (true) {
    KInquirer.promptInput(message).let {
      if (it.isNotBlank()) {
        return it
      }
    }
  }
}

// See https://spdx.org/licenses/
enum class SupportedLicenses(val fullName: String) {
  MIT("MIT License")
}