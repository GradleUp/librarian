package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import java.io.File

internal class CreateCommand : CliktCommand() {
  private val directory by argument()

  override fun run() {
    requireGh()
    with(File(directory)) {
      val gitHubRepository = promptGitHubRepository()
      init(gitHubRepository, defaultLicense)

      addLicense(defaultLicense.name, gitHubRepository.name)

      copyResource("gitignore", ".gitignore")
      copyResource("gradle.properties", "gradle.properties")

      copyResource("codeStyles/codeStyleConfig.xml", ".idea/codeStyles/codeStyleConfig.xml")
      copyResource("codeStyles/Project.xml", ".idea/codeStyles/Project.xml")

      copyResource("actions/check-pull-request.yaml", ".github/workflows/check-pull-request.yaml")
      copyResource("actions/prepare-release.yaml", ".github/workflows/prepare-release.yaml")
      copyResource("actions/publish-release.yaml", ".github/workflows/publish-release.yaml")
      copyResource("actions/publish-snapshot.yaml", ".github/workflows/publish-snapshot.yaml")

      /**
       * copied from https://github.com/spring-io/initializr/tree/fbbbe6734e55b4f6393624985c06161b16c9fe8f/initializr-generator-spring/src/main/resources/gradle/8
       * TODO: download latest version automatically
       */
      if (resolve("gradlew").exists().not()) {
        copyResource("gradlew", "gradlew", true)
        copyResource("gradlew.bat", "gradlew.bat", true)
        copyResource("gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.jar")
        copyResource("gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.properties")
      }

      if (resolve("gradle/libs.versions.toml").exists().not()) {
        resolve("gradle/libs.versions.toml").writeText("[libraries]", false)
      }

      println("✅ build.gradle.kts")
      resolve("build.gradle.kts").writeText(
          """
          import com.gradleup.librarian.gradle.librarianRoot
          
          plugins {
            id("org.jetbrains.kotlin.jvm").version("${latestKotlinRelease()}").apply(false)
            id("com.gradleup.librarian").version("${latestLibrarianRelease()}").apply(false)
          }
          
          librarianRoot()
          """.trimIndent(),
          overwrite = false
      )

      val moduleName = "module"
      resolve(moduleName).let { moduleDir ->
        moduleDir.mkdirs()

        println("✅ $moduleDir/build.gradle.kts")

        moduleDir.resolve("build.gradle.kts").writeText(
            """
        import com.gradleup.librarian.gradle.librarianModule
        
        plugins {
          id("org.jetbrains.kotlin.jvm")
        }
          
        librarianModule()  
        
      """.trimIndent()
        )

        println("✅ $moduleDir/README.md")

        moduleDir.resolve("README.md").writeText(
            """
          # Module $moduleName
        """.trimIndent()
        )
      }
      println("✅ settings.gradle.kts")
      resolve("settings.gradle.kts").writeText(
          """
        pluginManagement {
          listOf(repositories, dependencyResolutionManagement.repositories).forEach {
            it.apply {
              mavenCentral()
              google()
            }
          }
        }

        include(":$moduleName")
    """.trimIndent(), false
      )

      print("Initializing git repository...")
      runCommand("git", "init")
      runCommand("git", "add", ".")
      runCommand("git", "commit", "-a", "-m", "initial commit")

      val upload = KInquirer.promptConfirm(
          "Upload your project to GitHub at ${gitHubRepository.owner}/${gitHubRepository.name} and make it public?",
          default = true
      )
      if (upload) {
        runCommand("gh", "repo", "create", "--public", "-s", ".", "--push")
        setupGitHub()
      } else {
        println("run 'librarian setup-github' to finish configuration")
      }
    }
  }
}

