import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.gratatouille")
}

Librarian.module(project)

dependencies {
  api(libs.bcv)
  api(libs.dokkatoo)

  implementation(project(":librarian-core"))
  implementation(libs.coroutines)
  implementation(libs.vespene)
  implementation(libs.maven.sympathy)
  implementation(libs.okhttp)
  implementation(libs.google.auth)
  api(libs.nmcp)

  compileOnly(libs.gradle.api)
  compileOnly(libs.agp)
  compileOnly(libs.kgp)

  testImplementation(kotlin("test"))
  testImplementation(gradleTestKit())
  testImplementation(libs.mockserver)
}

gratatouille {
  pluginMarker("com.gradleup.librarian")
  codeGeneration {

  }
}
