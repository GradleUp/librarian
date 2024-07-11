package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.nio.file.Path

fun Path.initActions(runner: String) {
  val variableValues = mapOf("githubRunner" to runner)

  for (name in listOf("build-pull-request", "publish-docs", "publish-release", "publish-snapshot", "tag-and-bump")) {
    readResource("actions/$name.yaml", variableValues)
        .writeTo(resolve(".github/workflows/$name.yaml"))
  }
}