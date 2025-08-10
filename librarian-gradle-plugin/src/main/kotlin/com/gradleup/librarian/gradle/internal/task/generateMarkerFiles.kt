package com.gradleup.librarian.gradle.internal.task

import com.gradleup.librarian.gradle.internal.Dependencies
import com.gradleup.librarian.gradle.internal.Dependency
import com.gradleup.librarian.gradle.internal.Developer
import com.gradleup.librarian.gradle.internal.Developers
import com.gradleup.librarian.gradle.internal.License
import com.gradleup.librarian.gradle.internal.Licenses
import com.gradleup.librarian.gradle.internal.Project
import com.gradleup.librarian.gradle.internal.Scm
import com.gradleup.librarian.gradle.internal.digest
import com.gradleup.librarian.gradle.internal.serialize
import com.gradleup.librarian.gradle.internal.sign
import gratatouille.tasks.GInputFile
import gratatouille.tasks.GLogger
import gratatouille.tasks.GOutputDirectory
import gratatouille.tasks.GTask
import java.io.File
import java.time.Instant
import java.util.zip.ZipInputStream


private val re = Regex("META-INF/gradle-plugins/(.+).properties")

/**
 * Looks for Gradle plugins in [jar] resources and add them to the nmcp publishing task.
 *
 * @param mainArtifactId the name of the project
 */
@GTask
fun generateMarkerFiles(
  logger: GLogger,
  jar: GInputFile,
  mainGroupId: String,
  mainArtifactId: String,
  mainVersion: String,
  url: String,
  spdxLicenseId: String,
  developer: String,
  pluginIdsToIgnore: List<String>,
  privateKey: String?,
  privateKeyPassword: String?,
  output: GOutputDirectory
) {
  output.deleteRecursively()

  ZipInputStream(jar.inputStream()).use { zipInputStream ->
    while (true) {
      val entry = zipInputStream.nextEntry
      if (entry == null) {
        break
      }

      val match = re.matchEntire(entry.name)
      if (!entry.isDirectory && match != null) {
        val pluginId = match.groupValues[1]
        val artifactId = "$pluginId.gradle.plugin"

        if (pluginIdsToIgnore.contains(pluginId)) {
          continue
        }

        output.resolve(pluginId.asPath()).resolve(artifactId).resolve(mainVersion).apply {
          mkdirs()
          val project = Project(
            modelVersion = "4.0.0",
            groupId = pluginId,
            artifactId = artifactId,
            version = mainVersion,
            packaging = "pom",
            name = mainArtifactId,
            description = "Generated plugin marker file for $pluginId",
            url = url,
            licenses = Licenses(
              listOf(
                License(
                  name = spdxLicenseId,
                )
              )
            ),
            developers = Developers(
              listOf(
                Developer(
                  name = developer
                )
              )
            ),
            scm = Scm(
              connection = url,
              developerConnection = url,
              url = url
            ),
            dependencies = Dependencies(
              listOf(
                Dependency(
                  groupId = mainGroupId,
                  artifactId = mainArtifactId,
                  version = mainVersion
                )
              )
            )
          )

          val v = if (mainVersion.endsWith("-SNAPSHOT")) {
            // Do like Gradle and replace -SNAPSHOT by a timestamp and a buildNumber of 1
            "${mainVersion.substringBefore("-SNAPSHOT")}-${timestampNow()}-1"
          } else {
            mainVersion
          }
          val file = resolve("$artifactId-$v.pom")
          file.writeText(project.serialize())
          file.writeChecksums()
          if (privateKey != null) {
            check(privateKeyPassword != null) {
              "Librarian: a signing private key was set with its corresponding password."
            }
            val ascFile = file.writeSignature(privateKey, privateKeyPassword)
            ascFile.writeChecksums()
          }
        }
      }
    }
  }
}

private fun String.asPath() = replace('.', '/')

private fun File.writeSignature(privateKey: String, privateKeyPassword: String): File {
  val ascFile = parentFile.resolve(name.substringBeforeLast('.') + ".asc")
  inputStream().use { inputStream ->
    ascFile.writeText(inputStream.sign(privateKey, privateKeyPassword))
  }
  return ascFile
}

private fun File.writeChecksums() {
  listOf("md5", "sha1", "sha256", "sha512").forEach { algorithm ->
    inputStream().use {
      parentFile.resolve("$name.$algorithm").writeText(
        it.digest(algorithm.uppercase())
      )
    }
  }
}

internal fun timestampNow(): String {
  val now = Instant.now().atZone(java.time.ZoneOffset.UTC)

  return String.format("%04d%02d%02d.%02d%02d%02d", now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute, now.second)
}
