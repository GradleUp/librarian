package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.nio.file.Path

fun Path.initChangelog() {
  readResource("CHANGELOG.md")
      .writeTo(resolve("CHANGELOG.md"))
}