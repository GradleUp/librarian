package com.gradleup.librarian.core.tooling

import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.Path

private fun requireGh() {
  try {
    Path(".").runCommandAndCaptureStdout("gh", "--version")
  } catch (e: IOException) {
    throw Exception("Cannot run 'gh --version'. Is it installed? See https://cli.github.com/ for more details.", e)
  }
}

fun Path.GH(): GH {
  return GH(this)
}

class GH(private val path: Path) {
  init {
    requireGh()
  }

  fun repository(): GitHubRepository {
    return repositoryOrNull() ?: error("Cannot find GitHub repository")
  }
  fun repositoryOrNull(): GitHubRepository? {
    return path.runCommandAndCaptureStdout("gh", "repo", "view", "--json", "owner,name")
        .let {
          if (it.code != 0) {
            /*
             * Assume it's because we have no GitHub remote in this repository
             * XXX: fine tune error handling
             */
            return null
          }
          it.stdout
        }
        .toJsonElement()
        .run {
          GitHubRepository(
              owner = resolvePathAsStringOrNull("$.owner.login") ?: error("No repository owner found in '${this}'"),
              name = resolvePathAsStringOrNull("$.name") ?: error("No repository name found in '${this}'"),
          )
        }
  }

  fun setDescription(description: String) {
    path.runCommand("gh", "repo", "edit", "-d", description)
  }

  fun addTopics(topics: List<String>) {
    topics.forEach {
      path.runCommand("gh", "repo", "edit", "--add-topic", it)
    }
  }

  fun setUrl(url: String) {
    path.runCommand("gh", "repo", "edit", "-h", url)
  }

  fun setSecret(name: String, value: String) {
    path.runCommand("gh", "secret", "set", name, "-b", value)
  }

  fun allowWorkflowWrite() {
    val repository = repository()
    /**
     * Give the token write permissions
     */
    // https://docs.github.com/en/rest/actions/permissions?apiVersion=2022-11-28#set-default-workflow-permissions-for-a-repository
    path.runCommand(
        "gh",
        "api",
        "--method", "PUT",
        "-H", "Accept: application/vnd.github+json",
        "-H", "X-GitHub-Api-Version: 2022-11-28",
        "/repos/${repository.owner}/${repository.name}/actions/permissions/workflow",
        "-f", "default_workflow_permissions=write",
        "-F", "can_approve_pull_request_reviews=true"
    )
  }

  fun addTeam(team: String, permission: String) {
    val repository = repository()
    // Succeeds even if the team is already there
    path.runCommand(
      "gh",
      "api",
      "--method", "PUT",
      "-H", "Accept: application/vnd.github+json",
      "-H", "X-GitHub-Api-Version: 2022-11-28",
      "/orgs/${repository.owner}/teams/$team/repos/${repository.owner}/${repository.name}",
      "-f", "permission=$permission"
    )
  }

  fun createBranch(branch: String) {
    val repository = repository()
    var result = path.runCommandAndCaptureStdout(
      "gh",
      "api",
      "--method", "GET",
      "-H", "Accept: application/vnd.github+json",
      "-H", "X-GitHub-Api-Version: 2022-11-28",
      "/repos/${repository.owner}/${repository.name}/git/ref/heads/main",
      "--jq", ".object.sha"
    )
    require(result.code == 0) {
      "Cannot get main sha1: '${result.stdout}'"
    }

    result = path.runCommandAndCaptureStdout(
      "gh",
      "api",
      "--method", "POST",
      "-H", "Accept: application/vnd.github+json",
      "-H", "X-GitHub-Api-Version: 2022-11-28",
      "/repos/${repository.owner}/${repository.name}/git/refs",
      "-f", "ref=refs/heads/$branch",
      "-f", "sha=${result.stdout.trim()}"
    )
    println("branch '$branch' created!")
    println(result.code)
    println(result.stdout)
  }

  fun enablePages(branch: String) {
    val repository = repository()
    val result = path.runCommandAndCaptureStdout(
        "gh",
        "api",
        "--method", "POST",
        "-H", "Accept: application/vnd.github+json",
        "-H", "X-GitHub-Api-Version: 2022-11-28",
        "/repos/${repository.owner}/${repository.name}/pages",
        "-f", "source[branch]=$branch",
    )
    println("pages '$branch' enabled")
    println(result.code)
    println(result.stdout)
  }
}

class GitHubRepository(val owner: String, val name: String)


fun getAvailableOrganizations(): List<String> {
  return with(Path(".")) {
    val username = runCommandAndCaptureStdout("gh", "api", "user", "--jq", ".login").stdoutOrThrow().trim()
    val organisations = runCommandAndCaptureStdout("gh", "org", "list").stdoutOrThrow().lines().filter {
      it.isNotBlank() && !it.startsWith("Showing ")
    }
    listOf(username) + organisations
  }
}


