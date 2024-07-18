package com.gradleup.librarian.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import com.gradleup.librarian.cli.command.init.setSecrets
import com.gradleup.librarian.cli.promptMultilinePassword
import com.gradleup.librarian.cli.requirePassword
import com.gradleup.librarian.core.tooling.GH
import com.gradleup.librarian.core.tooling.GitHubRepository
import com.gradleup.librarian.core.tooling.getAvailableOrganizations
import com.gradleup.librarian.core.tooling.init.Secrets
import com.gradleup.librarian.core.tooling.init.initMetadata
import com.gradleup.librarian.core.tooling.init.initSecrets
import com.gradleup.librarian.core.tooling.repositoryOrNull
import com.gradleup.librarian.core.tooling.runCommand
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.name

internal class UploadResult(val uploaded: Boolean, val secretsSet: Boolean)

class UploadCommand : CliktCommand() {
  override fun run() {

    Path(".").apply {
      val gitHubProjectName = KInquirer.promptInput(message = "GitHub repository name", this.absolute().normalize().name)
      val gitHubProjectOwner = KInquirer.promptList(message = "GitHub repository owner", getAvailableOrganizations())
      val repository = GitHubRepository(gitHubProjectOwner, gitHubProjectName)
      upload(repository, "")
    }
  }
}

internal fun Path.upload(repository: GitHubRepository, defaultDescription: String): UploadResult {
  val upload = KInquirer.promptConfirm(
      "Upload your project to GitHub at ${repository.owner}/${repository.name} and make it public?",
      default = true
  )
  if (!upload) {
    return UploadResult(false, false)
  }

  runCommand("gh", "repo", "create", "--public", "-s", ".", "--push")

  val gitHubDescription = KInquirer.promptInput("GitHub description", defaultDescription)
  val gitHubTopics = KInquirer.promptInput("GitHub topics (use comma separated list)", "kotlin").split(",").map { it.trim() }
  println("Setting GitHub metadata...")
  initMetadata(gitHubDescription, null, gitHubTopics)

  val gh = GH()
  println("Giving write permissions to the GitHub actions workflows...")
  gh.allowWorkflowWrite()

  println("Enabling GitHub pages on branch `gh-pages`...")
  gh.createBranch("gh-pages")
  gh.enablePages("gh-pages")

  val secrets = KInquirer.promptConfirm(
      "Set your project secrets now?",
      default = true
  )
  if (!secrets) {
    return UploadResult(true, false)
  }

  setSecrets()

  return UploadResult(true, true)
}