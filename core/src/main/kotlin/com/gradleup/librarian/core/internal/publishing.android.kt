package com.gradleup.librarian.core.internal

import com.android.build.api.dsl.LibraryExtension
import com.gradleup.librarian.core.internal.androidExtension
import org.gradle.api.Project

fun Project.createAndroidPublication(variantName: String) {
  val android = androidExtension
  check (android is LibraryExtension) {
    "Librarian: cannot publish non-library project"
  }
  android.publishing {
    singleVariant(variantName) {
      withSourcesJar()
    }
  }
}
