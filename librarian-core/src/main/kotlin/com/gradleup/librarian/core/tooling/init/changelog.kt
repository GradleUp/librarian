package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.readTextResource
import com.gradleup.librarian.core.tooling.writeTextTo
import java.nio.file.Path

fun Path.initChangelog() {
  readTextResource("CHANGELOG.md")
      .writeTextTo(resolve("CHANGELOG.md"))
}