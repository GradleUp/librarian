package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.GitHubRepository
import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories

fun Path.initWriterside(repository: GitHubRepository) {
  val variables = mapOf(
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
    readResource(it, variables).writeTo(resolve(it))
  }
  Path("Writerside").createParentDirectories()
}