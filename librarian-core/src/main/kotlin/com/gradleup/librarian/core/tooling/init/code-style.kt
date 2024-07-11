package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.nio.file.Path


fun Path.initCodeStyle(indent: String) {
  val variableValues = mapOf("indent" to indent, "continuationIndent" to indent.toInt().times(2).toString())

  for (name in listOf("codeStyleConfig.xml", "Project.xml")) {
    readResource("codeStyles/$name", variableValues)
        .writeTo(resolve(".idea/codeStyles/$name"))
  }
}