package com.gradleup.librarian.gradle.internal

import org.gradle.api.Project

internal val Project.hasAndroid: Boolean get() = extensions.findByName("android") != null

internal val Project.hasKotlin: Boolean get() = extensions.findByName("kotlin") != null

internal fun Project.findGradleProperty(name: String): String? {
  return rootProject.findProperty(name)?.let {
    if (it !is String) {
      error("Librarian: gradle property '$name' is not a String (found '$it')")
    }
    it
  }
}

internal fun Project.findEnvironmentVariable(name: String): String? {
  return providers.environmentVariable(name).orNull
}
