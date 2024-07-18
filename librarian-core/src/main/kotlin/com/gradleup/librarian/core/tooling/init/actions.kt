package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.nio.file.Path

fun Path.initActions(runner: String, addDocumentationSite: Boolean) {
  val variableValues = mapOf("githubRunner" to runner)

  val docsWorkflow = if (addDocumentationSite) {
    "publish-docs"
  } else {
    "publish-kdoc"
  }
  for (name in listOf("build-pull-request", "publish-release", "publish-snapshot", "tag-and-bump", docsWorkflow)) {
    readResource("actions/$name.yaml", variableValues)
        .writeTo(resolve(".github/workflows/$name.yaml"))
  }
}