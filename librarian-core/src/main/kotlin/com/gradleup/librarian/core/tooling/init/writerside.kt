package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.GitHubRepository
import com.gradleup.librarian.core.tooling.readTextResource
import com.gradleup.librarian.core.tooling.writeTextTo
import java.nio.file.Path

fun Path.initWriterside(repository: GitHubRepository) {
  val variables = mapOf(
      "projectTitle" to repository.name,
      "repositoryName" to repository.name,
      "repositoryOwner" to repository.owner,
  )
  listOf(
      "Writerside/cfg/buildprofiles.xml",
      "Writerside/topics/welcome.md",
      "Writerside/c.list",
      "Writerside/doc.tree",
      "Writerside/v.list",
      "Writerside/writerside.cfg",
  ).forEach {
    readTextResource(it, variables).writeTextTo(resolve(it))
  }
}