package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.VERSION
import com.gradleup.librarian.core.tooling.GitHubRepository
import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.nio.file.Path

fun Path.initProject(multiplatform: Boolean, projectTitle: String, groupId: String, artifactId: String, repository: GitHubRepository) {
  val variables = mapOf(
      "groupId" to groupId,
      "artifactId" to artifactId,
      "repositoryOwner" to repository.owner,
      "repositoryName" to repository.name,
      "projectTitle" to projectTitle,
      "kotlinPluginId" to if (multiplatform) "org.jetbrains.kotlin.multiplatform" else "org.jetbrains.kotlin.jvm",
      "librarianVersion" to VERSION
  )

  readResource("project/gradle.properties").writeTo(resolve("gradle.properties"))
  readResource("project/README.md", variables).writeTo(resolve("README.md"))
  readResource("project/gradle/libs.versions.toml").writeTo(resolve("gradle/libs.versions.toml"))
  readResource("project/build.gradle.kts", variables).writeTo(resolve("build.gradle.kts"))
  readResource("project/module/build.gradle.kts", variables).writeTo(resolve("module/build.gradle.kts"))
}

