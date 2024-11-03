# Librarian

Librarian is an opinionated toolkit to build Kotlin libraries. Librarian features:

* [Configuration-cache](https://docs.gradle.org/current/userguide/configuration_cache.html), [isolated projects](https://docs.gradle.org/current/userguide/isolated_projects.html) compatible publishing of KMP/Android/JVM libraries to:
  * Maven Central using [default host](https://central.sonatype.org/publish/publish-guide/#releasing-to-central)
  * Maven Central using [S01 host](https://central.sonatype.org/publish/publish-guide/#releasing-to-central)
  * Maven Central using [the central portal API](https://central.sonatype.com/api-doc)
  * OSS Sonatype snapshots using the default host
  * OSS Sonatype snapshots using the S01 host
  * [Google Cloud Storage](https://cloud.google.com/storage?hl=en)
* Versioned KDoc using [Dokka](https://github.com/Kotlin/dokka) 
* Java and Kotlin compatibility helpers
* GitHub actions for testing, publishing and documentation
* Automatic generation of a symbol for your library version
* Binary Compatibility validator integration
* and more

Librarian is highly modular and has several parts that can be used independently:

* The `com.gradleup.librarian` Gradle plugin is an all-in-one plugin that configures sensible defaults for your library. 
* The `librarian-cli` application is a companion CLI that lets you deal with several aspects of daily library development.  
* The libraries contain the code that powers the `com.gradleup.librarian` plugin and can be used standalone. 

## Get started

To add librarian to your root project: 