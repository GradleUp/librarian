package com.gradleup.librarian.gradle

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * From https://github.com/arrow-kt/arrow-gradle-config/blob/main/arrow-gradle-config-publish/src/main/kotlin/internal/PublishMppRootModuleInPlatform.kt
 */
fun Project.publishPlatformArtifactsInRootModule() = afterEvaluate {
  if (kotlinExtension !is KotlinMultiplatformExtension) {
    return@afterEvaluate
  }
  val platformPublication = extensions.getByType(PublishingExtension::class.java)
      .publications
      .findByName("jvm") as MavenPublication?

    if (platformPublication == null) {
        return@afterEvaluate
    }

  lateinit var platformXml: XmlProvider
  platformPublication.pom?.withXml { platformXml = it }

  extensions
      .findByType(PublishingExtension::class.java)
      ?.publications
      ?.getByName("kotlinMultiplatform")
      ?.let { it as MavenPublication }
      ?.run {
        // replace pom
        pom.withXml {
          val xmlProvider = it
          val root = xmlProvider.asNode()
          // Remove the original content and add the content from the platform POM:
          root.children().toList().forEach { root.remove(it as Node) }
          platformXml.asNode().children().forEach { root.append(it as Node) }

          // Adjust the self artifact ID, as it should match the root module's coordinates:
          ((root.get("artifactId") as NodeList).get(0) as Node).setValue(artifactId)

          // Set packaging to POM to indicate that there's no artifact:
          root.appendNode("packaging", "pom")

          // Remove the original platform dependencies and add a single dependency on the platform
          // module:
          val dependencies = (root.get("dependencies") as NodeList).get(0) as Node
          dependencies.children().toList().forEach { dependencies.remove(it as Node) }
          val singleDependency = dependencies.appendNode("dependency")
          singleDependency.appendNode("groupId", platformPublication.groupId)
          singleDependency.appendNode("artifactId", platformPublication.artifactId)
          singleDependency.appendNode("version", platformPublication.version)
          singleDependency.appendNode("scope", "compile")
        }
      }

  tasks
      .matching { it.name == "generatePomFileForKotlinMultiplatformPublication" }
      .configureEach {
        it.dependsOn("generatePomFileFor${platformPublication.name.capitalized()}Publication")
      }
}

