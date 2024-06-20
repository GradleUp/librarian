package com.gradleup.librarian.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.kinquirer.KInquirer
import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import kotlinx.serialization.json.Json
import java.io.File

internal class SetupGithubCommand: CliktCommand() {
  override fun run() {
    File(".").setupGitHub()
  }
}

internal fun File.setupGitHub() {
  with(this) {
    val repo = getCurrentRepo()
    check (repo != null) {
      "No GitHub remote found. Run 'gh repo create'"
    }

    /**
     * Give the token write permissions
     */
    // https://docs.github.com/en/rest/actions/permissions?apiVersion=2022-11-28#set-default-workflow-permissions-for-a-repository
    runCommand(
        "gh",
        "api",
        "--method", "PUT",
        "-H", "Accept: application/vnd.github+json",
        "-H", "X-GitHub-Api-Version: 2022-11-28",
        "/repos/${repo.owner}/${repo.name}/actions/permissions/workflow",
        "-f", "default_workflow_permissions=write",
        "-F", "can_approve_pull_request_reviews=true"
    )

    /**
     * Set GitHub secrets
     */
    println("Paste your armoured GPG key beginning with '-----BEGIN PGP PRIVATE KEY BLOCK-----' (press Enter 3 times when done)")
    val gpgKey = KInquirer.promptMultilinePassword("LIBRARIAN_SIGNING_PRIVATE_KEY")

    println("setting secret...")
    runCommand("gh", "secret", "set", "LIBRARIAN_SIGNING_PRIVATE_KEY", "-b", gpgKey)
    listOf("LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD", "LIBRARIAN_SONATYPE_USERNAME", "LIBRARIAN_SONATYPE_PASSWORD").forEach {
      // Consume extra keystroke buffered while the previous `gh` command executes
      println("setting secret...")
      runCommand("gh", "secret", "set", it, "-b", requirePassword(it))
    }
  }
}
internal class Repo(val owner: String, val name: String)

internal fun File.getCurrentRepo(): Repo? {
  val result = runCommandAndCaptureStdout("gh", "repo", "view", "--json", "owner,name")

  if (result.code != 0) {
    return null
  }

  val jsonElement = Json.parseToJsonElement(result.stdout)

  val name = jsonElement.resolvePathAsStringOrNull("$.name")
  check(name != null) {
    "Cannot find 'name' in ${result.stdout}"
  }
  val owner = jsonElement.resolvePathAsStringOrNull("$.owner.login")
  check(owner != null) {
    "Cannot find 'owner.login' in ${result.stdout}"
  }

  return Repo(owner, name)
}