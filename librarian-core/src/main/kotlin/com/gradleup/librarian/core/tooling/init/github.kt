package com.gradleup.librarian.core.tooling.init

import com.gradleup.librarian.core.tooling.GH
import java.nio.file.Path


fun Path.initGitHub() {
  val gh = GH()
  gh.allowWorkflowWrite()
  gh.createBranch("gh-pages")
  gh.enablePages("gh-pages")
}
