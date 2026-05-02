import com.gradleup.librarian.gradle.Librarian
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

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
  compileOnly(libs.kgp220)

  testImplementation(kotlin("test"))
  testImplementation(gradleTestKit())
  testImplementation(libs.mockserver)
}

private fun addEdge(compilation: KotlinCompilation<*>, dependency: KotlinCompilation<*>) {
  compilation.defaultSourceSet.dependencies {
    compileOnly(dependency.output.classesDirs)
  }
}

val mainCompilation = kotlin.target.compilations.getByName("main")

mapOf(
  "220" to setOf(libs.kgp220),
  "2320" to setOf(libs.kgp2320),
  "240" to setOf(libs.kgp240)
).forEach {
  val compilation = kotlin.target.compilations.create("kgp-${it.key}")

  addEdge(mainCompilation, compilation) // Needed to be able

  tasks.jar {
    from(compilation.output.classesDirs)
  }
  dependencies {
    it.value.forEach {
      add(compilation.compileOnlyConfigurationName, it)
    }
    // See https://issuetracker.google.com/issues/445209309
    add(compilation.compileOnlyConfigurationName, libs.gradle.api)
  }
}