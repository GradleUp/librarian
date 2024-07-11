package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.makeExecutable
import com.gradleup.librarian.core.tooling.readResource
import com.gradleup.librarian.core.tooling.writeTo
import java.nio.file.Path

fun Path.initGradleWrapper() {
  readResource("gradle/gradle-wrapper.jar").writeTo(resolve("gradle/wrapper/gradle-wrapper.jar"))
  readResource("gradle/gradle-wrapper.properties").writeTo(resolve("gradle/wrapper/gradle-wrapper.properties"))
  readResource("gradle/gradlew").writeTo(resolve("gradle/wrapper/gradlew"))
  readResource("gradle/gradlew.bat").writeTo(resolve("gradle/wrapper/gradlew.bat"))

  resolve("gradle/wrapper/gradlew").makeExecutable()
  resolve("gradle/wrapper/gradlew.bat").makeExecutable()
}