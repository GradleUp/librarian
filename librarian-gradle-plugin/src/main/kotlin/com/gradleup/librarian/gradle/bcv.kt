package com.gradleup.librarian.gradle

import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.ExperimentalBCVApi
import org.gradle.api.Project

@OptIn(ExperimentalBCVApi::class)
fun Project.configureBcv(block: ApiValidationExtension.() -> Unit = {}) {
  pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")
  extensions.getByType(ApiValidationExtension::class.java).apply {
    klib.enabled = true
    block()
  }
}