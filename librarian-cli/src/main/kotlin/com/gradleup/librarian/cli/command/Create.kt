package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.gradleup.librarian.core.tooling.GitHubRepository
import com.gradleup.librarian.core.tooling.getAvailableOrganizations
import com.gradleup.librarian.core.tooling.init.SonatypeBackend
import com.gradleup.librarian.core.tooling.init.SupportedLicense
import com.gradleup.librarian.core.tooling.init.currentYear
import com.gradleup.librarian.core.tooling.init.initActions
import com.gradleup.librarian.core.tooling.init.initChangelog
import com.gradleup.librarian.core.tooling.init.initCodeStyle
import com.gradleup.librarian.core.tooling.init.initGitIgnore
import com.gradleup.librarian.core.tooling.init.initGradleWrapper
import com.gradleup.librarian.core.tooling.init.initLibrarian
import com.gradleup.librarian.core.tooling.init.initLicense
import com.gradleup.librarian.core.tooling.init.initProject
import com.gradleup.librarian.core.tooling.init.initWriterside
import com.gradleup.librarian.core.tooling.init.toBaseUrl
import com.gradleup.librarian.core.tooling.init.toSupportedLicense
import com.gradleup.librarian.core.tooling.runCommand
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.system.exitProcess

internal class Create : CliktCommand() {
  private val directory by argument()

  @OptIn(ExperimentalPathApi::class)
  override fun run() {

    with(Path(directory)) {
      if (exists()) {
        if(KInquirer.promptConfirm("'$directory' already exists. Overwrite?", false)) {
          deleteRecursively()
        } else {
          exitProcess(1)
        }
      }
      createDirectory()

      println("""
        
        📚 Welcome to librarian 📚
        
        Librarian helps you build and maintain Kotlin libraries.
        
        ℹ️  Librarian requires a GitHub repository. 
        
        We'll now ask a few questions and prepare a brand new project in the '$directory' directory. 
        Once this is done, you'll have a chance to review the project and push it to GitHub (or do it later).
        
      """.trimIndent())

      val gitHubProjectName = KInquirer.promptInput(message = "GitHub repository name", directory)
      val gitHubProjectOwner = KInquirer.promptList(message = "GitHub repository owner", getAvailableOrganizations())
      val license = KInquirer.promptList("License", SupportedLicense.entries.map { it.name }).toSupportedLicense()
      val copyrightHolder = KInquirer.promptInput("Copyright holder", "$gitHubProjectName authors")
      val groupId = KInquirer.promptInput("Maven group id", "io.github.${gitHubProjectOwner}.${gitHubProjectName}")
      val pomDescription = KInquirer.promptInput("Maven pom description")
      val pomDeveloper = KInquirer.promptInput("Maven pom developer", copyrightHolder)
      val sonatypeBackend = KInquirer.promptList("Sonatype backend", SonatypeBackend.entries.map { it.name })
      val multiplatform = KInquirer.promptConfirm("Kotlin multiplatform project")
      val javaCompatibility = KInquirer.promptInput("Java compatibility", "8")
      val kotlinCompatibility = KInquirer.promptInput("Kotlin compatibility", "2.0.0")
      val indent = KInquirer.promptInput("Indent size", "4")
      val addDocumentationSite = KInquirer.promptConfirm("Add Writerside documentation site?", true)

      val repository = GitHubRepository(gitHubProjectOwner, gitHubProjectName)
      val backend = SonatypeBackend.valueOf(sonatypeBackend)
      print("Writing files...")

      initLicense(license, currentYear(), copyrightHolder)
      initChangelog()
      initLibrarian(javaCompatibility,
          kotlinCompatibility,
          backend,
          groupId,
          repository,
          SupportedLicense.MIT,
          pomDescription,
          pomDeveloper
      )
      if (addDocumentationSite) {
        initWriterside(repository)
      }
      initActions(if (multiplatform) "macos-latest" else "ubuntu-latest", addDocumentationSite)

      /**
       * Below are the things that are specific to "create" and run only once for a given project
       */
      initCodeStyle(indent)
      initGitIgnore()
      initGradleWrapper()
      initProject(
          multiplatform,
          repository.name,
          pomDescription,
          groupId,
          "module",
          repository,
          addDocumentationSite,
          backend
      )

      runCommand("git", "init")
      runCommand("git", "add", ".")
      runCommand("git", "commit", "-a", "-m", "initial commit")

      print("""
        
        Your project is now created ✅
        Peek around and if everything looks good, make it public!
        
      """.trimIndent())

      val result = upload(repository, pomDescription)

      println("All done \uD83C\uDF89")
      println("Next steps:")
      if (!result.uploaded) {
        println("- run `librarian upload` to upload your project to GitHub")
      }
      if (!result.secretsSet) {
        println("- run `librarian init secrets` to set your publishing secrets")
      }
      println("- push commits to `main`")
      if (addDocumentationSite) {
        println("- browse Writerside documentation at https://${gitHubProjectOwner}.github.io/$gitHubProjectName/")
      }
      println("- browse KDoc API reference at https://${gitHubProjectOwner}.github.io/$gitHubProjectName/kdoc")
      when (backend) {
        SonatypeBackend.S01, SonatypeBackend.Default -> {
          println("- browse SNAPSHOTs at ${backend.toBaseUrl()}/content/repositories/snapshots/${groupId.replace('.', '/')}")
        }
        else -> {}
      }
      println("- run 'librarian triggerTagAndBump' to kickoff your first release \uD83D\uDE80")
    }
  }
}
