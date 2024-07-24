package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.GH
import com.gradleup.librarian.core.tooling.gitHubRepositoryOrNull
import java.nio.file.Path

fun Path.initMetadata(description: String, url: String?, topics: List<String>) {
  val gh = GH()
  var url2 = url
  if (url2 == null) {
    val repository = gitHubRepositoryOrNull() ?: error("No GitHub repository found")
    url2 = "https://${repository.owner}.github.io/${repository.name}/"
  }

  gh.setDescription(description)
  gh.addTopics(topics)
  gh.setUrl(url2)
}
