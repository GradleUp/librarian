---
title: Welcome to Librarian
---

Librarian is an opinionated toolkit to build Kotlin libraries.

:::note
This project is work in progress, this documentation itself is work in progress.
If you have questions, please ask in the `#gradleup` channel in the [Gradle community Slack](https://gradle-community.slack.com/).

:::

Librarian:

* creates default publications based on applied plugins  
* publishes your KMP/Android/JVM libraries to:
    * Maven Central releases
    * Maven Central snapshots 
    * [Google Cloud Storage](https://cloud.google.com/storage?hl=en)
* publishes versioned KDoc using [Dokka](https://github.com/Kotlin/dokka)
* generates a Kotlin property holding your library version
* configures Binary Compatibility validator
* configures Java and Kotlin compatibility
* configures maven compatibility for your lib
* and more!


## Get started

### 1/4 Add it to your project

Add the Gradle plugin to your root `build.gradle.kts`:

```kotlin
plugins {
  id("com.gradleup.librarian").apply(false)
}

Librarian.root(project)
```

### 2/4 Root project configuration

Add a `librarian.root.properties` file to the root of your repository:

```
# Common information for your repository
pom.groupId=com.example
pom.version=0.0.7-SNAPSHOT
pom.description=Cool library
pom.vcsUrl=https://github.com/example/cool
pom.developer=Cool library authors
pom.license=MIT

# Optional: publish to Google Cloud Storage
gcs.bucket=gradleup
gcs.prefix=m2

# Optional: configure compatibility
java.compatibility=11 
kotlin.compatibility=2.0.0

# Optional: publish older versions of the Kdoc (requires them to be already published)
kdoc.olderVersions=1.0.0,2.0.0
```

### 3/4 Project specific configuration

Configure librarian in each subproject:

```kotlin
// module/build.gradle.kts
plugins {
  // no need to add librarian here, it's already on the classpath
}

// Configure librarian
Librarian.module(project)
```

### 4/4 Add the signing and publishing secrets

Add secrets to your environment:

```
export LIBRARIAN_SONATYPE_PASSWORD=...
export LIBRARIAN_SONATYPE_USERNAME=...
export LIBRARIAN_SIGNING_PRIVATE_KEY=...
export LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD=...
export LIBRARIAN_GOOGLE_SERVICES_JSON=...
```

### You can now publish the library!

Publish your library:

```
./gradlew librarianPublishToMavenCentral
./gradlew librarianPublishToMavenSnapshots
./gradlew librarianPublishToGcs
```

