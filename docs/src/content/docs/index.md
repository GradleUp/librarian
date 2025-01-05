---
title: Welcome to Librarian
---

Librarian is an opinionated toolkit to build Kotlin libraries.

:::note
This project is work in progress

:::

Librarian:

* publishes your KMP/Android/JVM libraries to:
    * Maven Central using [default host](https://central.sonatype.org/publish/publish-guide/#releasing-to-central)
    * Maven Central using [S01 host](https://central.sonatype.org/publish/publish-guide/#releasing-to-central)
    * Maven Central using [the central portal API](https://central.sonatype.com/api-doc)
    * OSS Sonatype snapshots using the default host
    * OSS Sonatype snapshots using the S01 host
    * [Google Cloud Storage](https://cloud.google.com/storage?hl=en)
    * GitHub pages snapshots
* generates versioned KDoc using [Dokka](https://github.com/Kotlin/dokka)
* automates your workflows using GitHub actions:
    * pull requests
    * documentation
    * SNAPSHOTs
    * releases
* generates a Kotlin property holding your library version
* monitors your binary compatibility using Kotlin Binary Compatibility validator
* configures Java and Kotlin compatibility
* ensure your libs can be consumed with maven
* manages your changelog
* and more!

This is a lot of things for a single tool, and a lot of opinions too!

Because every lib is different and opinions may differ, Librarian is also shipped as low level libraries that you can use on a case by case basis.

Just want the publishing without the GitHub workflows, use `librarian-publishing`. Just want maven sympathy, use `librarian-maven-sympathy`, etc... For more information, read the [libraries page](libraries.md).

## Get started

Add the Gradle plugin to your root `build.gradle.kts`:

```kotlin
plugins {
  id("com.gradleup.librarian")
}

Librarian.root(project)
```

Add a librarian.root.properties:

```
# Maven Central backend to use
# Valid values: Default, S01, Portal
sonatype.backend=Default

# Common POM (Project Object Model) information for your repository
pom.groupId=com.gradleup.librarian
pom.version=0.0.7-SNAPSHOT
pom.description=Librarian
pom.vcsUrl=https://github.com/gradleup/librarian
pom.developer=GradleUp authors
pom.license=MIT License

# Optional: publish to Google Cloud Storage
gcs.bucket=gradleup
gcs.prefix=m2

# Optional: configure java compatibility
java.compatibility=11 
kotlin.compatibility=2.0.0

# Optional: publish several versions of the Kdoc (requires them to be already published)
kdoc.olderVersions=1.0.0,2.0.0
```

Configure librarian in each module:

```kotlin
// module/build.gradle.kts
plugins {
  id("org.jetbrains.kotlin.jvm")
  // no need to add librarian here, it's already on the classpath
}

// Configure librarian
Librarian.module(project)
```

Add secrets to your environment:

```
export LIBRARIAN_SONATYPE_PASSWORD=...
export LIBRARIAN_SONATYPE_USERNAME=...
export LIBRARIAN_SIGNING_PRIVATE_KEY=...
export LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD=...
export LIBRARIAN_GOOGLE_SERVICES_JSON=...
```

Publish your library:

```
./gradlew librarianPublishToMavenCentral
./gradlew librarianPublishToMavenSnapshots
./gradlew librarianPublishToGcs
```

Automate your workflows:

