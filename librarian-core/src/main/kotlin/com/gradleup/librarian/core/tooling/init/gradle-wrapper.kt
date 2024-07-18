package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.makeExecutable
import com.gradleup.librarian.core.tooling.readBinaryResource
import com.gradleup.librarian.core.tooling.readTextResource
import com.gradleup.librarian.core.tooling.writeTextTo
import java.nio.file.Path

fun Path.initGradleWrapper() {
  readTextResource("gradle/gradle-wrapper.properties").writeTextTo(resolve("gradle/wrapper/gradle-wrapper.properties"))
  readBinaryResource("gradle/gradle-wrapper.jar").writeBinaryTo(resolve("gradle/wrapper/gradle-wrapper.jar"))
  readTextResource("gradle/gradlew").writeTextTo(resolve("gradlew"))
  readTextResource("gradle/gradlew.bat").writeTextTo(resolve("gradlew.bat"))

  resolve("gradlew").makeExecutable()
  resolve("gradlew.bat").makeExecutable()
}