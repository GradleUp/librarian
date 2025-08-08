#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.0.11-SNAPSHOT-0c63531f2132a26ec9e2f7f730ba41d74598e100")

updateRepo(args) {
    file("README.md") {
        replacePluginId("com.gradleup.librarian")
    }
}
