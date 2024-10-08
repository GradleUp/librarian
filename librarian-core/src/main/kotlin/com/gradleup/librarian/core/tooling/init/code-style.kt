package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.readTextResource
import com.gradleup.librarian.core.tooling.writeTextTo
import java.nio.file.Path


fun Path.initCodeStyle(indent: String) {
  val variableValues = mapOf("indent" to indent, "continuationIndent" to indent.toInt().times(2).toString())

  for (name in listOf("codeStyleConfig.xml", "Project.xml")) {
    readTextResource("codeStyles/$name", variableValues)
        .writeTextTo(resolve(".idea/codeStyles/$name"))
  }
}