#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("file://~/.m2/repository")
@file:Repository("https://dl.google.com/android/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.2.2-SNAPSHOT")

import com.gradleup.librarian.repo.updateRepo

updateRepo(args) {
  file("README.md") {
    replacePluginVersion("com.gradleup.librarian")
  }
}
