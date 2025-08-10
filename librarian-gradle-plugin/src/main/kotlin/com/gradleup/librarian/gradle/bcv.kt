package com.gradleup.librarian.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

@OptIn(ExperimentalAbiValidation::class)
fun Project.configureBcv(block: AbiValidationExtension.() -> Unit = {}) {
  extensions.getByType(KotlinProjectExtension::class.java).apply {
      this.extensions.getByName("abiValidation").apply {
          this as AbiValidationExtension
          enabled.set(true)
          block()
      }
  }
  tasks.named("build") {
    it.dependsOn("checkLegacyAbi")
  }
}