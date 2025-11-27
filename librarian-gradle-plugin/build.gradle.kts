import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.gratatouille")
  id("org.jetbrains.kotlin.plugin.serialization")
}

Librarian.module(project)

dependencies {
  api(libs.dokka)

  implementation(project(":librarian-core"))
  implementation(libs.coroutines)
  implementation(libs.maven.sympathy)
  implementation(libs.okhttp)
  implementation(libs.google.auth)
  implementation(libs.bcv)

  implementation(libs.bouncycastle.pg)
  implementation(libs.bouncycastle.prov)
  implementation(libs.serialization.json)
  implementation(libs.xmlutil)

  implementation(libs.tapmoc)
  api(libs.nmcp)
  api(libs.nmcp.tasks) {
    because("we use the publishFileByFile API")
  }

  compileOnly(libs.gradle.api)
  compileOnly(libs.agp)
  compileOnly(libs.kgp.compile.only)

  testImplementation(kotlin("test"))
  testImplementation(gradleTestKit())
  testImplementation(libs.mockserver)
}

gratatouille {
  codeGeneration {
  }
}
