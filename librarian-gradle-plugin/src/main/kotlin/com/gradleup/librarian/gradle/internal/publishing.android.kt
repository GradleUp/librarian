package com.gradleup.librarian.gradle.internal

import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.tasks.SourceJarTask
import com.gradleup.librarian.gradle.LIBRARIAN_GENERATE_VERSION
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider

fun Project.createAndroidPublication(variantName: String, javadocTaskProvider: TaskProvider<*>) {
  val android = androidExtension
  check (android is LibraryExtension) {
    "Librarian: cannot publish non-library project"
  }
  android.publishing {
    singleVariant(variantName) {
      withSourcesJar()
    }
  }

  extensions.getByType(PublishingExtension::class.java).apply {
    publications.register("default", MavenPublication::class.java) { publication ->
      afterEvaluate {
        publication.from(components.getByName(variantName))
        publication.artifact(javadocTaskProvider)
      }
    }
  }
  tasks.withType(SourceJarTask::class.java){ task ->
    task.dependsOn(LIBRARIAN_GENERATE_VERSION)
  }
}
