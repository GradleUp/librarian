package com.gradleup.librarian.gradle

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * From https://github.com/arrow-kt/arrow-gradle-config/blob/main/arrow-gradle-config-publish/src/main/kotlin/internal/PublishMppRootModuleInPlatform.kt
 */
internal fun Project.publishPlatformArtifactsInRootModule(group: String, version: String) = afterEvaluate {
  if (kotlinExtension !is KotlinMultiplatformExtension) {
    return@afterEvaluate
  }

  val publication = extensions
    .findByType(PublishingExtension::class.java)
    ?.publications
    ?.getByName("kotlinMultiplatform")
    ?.let { it as MavenPublication }

  if (publication == null) {
    return@afterEvaluate
  }
  // Redirect to the JVM publication
  val artifactId = publication.artifactId + "-jvm"

  publication.pom.withXml { xmlProvider ->
    val root = xmlProvider.asNode()
    // Set packaging to POM to indicate that there's no artifact:
    root.appendNode("packaging", "pom")

    // Remove the original platform dependencies and add a single dependency on the platform
    // module:
    val dependencies = (root.get("dependencies") as NodeList).get(0) as Node
    dependencies.children().toList().forEach { dependencies.remove(it as Node) }
    val singleDependency = dependencies.appendNode("dependency")
    singleDependency.appendNode("groupId", group)
    singleDependency.appendNode("artifactId", artifactId)
    singleDependency.appendNode("version", version)
    singleDependency.appendNode("scope", "compile")
  }
}

