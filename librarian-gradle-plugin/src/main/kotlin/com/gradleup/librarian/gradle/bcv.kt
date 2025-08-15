package com.gradleup.librarian.gradle

import com.gradleup.librarian.core.tooling.init.kotlinPluginVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import java.util.Properties

@OptIn(ExperimentalAbiValidation::class)
internal fun Project.configureBcv(properties: Properties?) {
  extensions.getByType(KotlinProjectExtension::class.java).apply {
    val abiValidation = this.extensions.findByName("abiValidation")

    if (abiValidation != null) {
      abiValidation as AbiValidationExtension
      abiValidation.enabled.set(true)

      tasks.named("build") {
        it.dependsOn("checkLegacyAbi")
      }
    } else {
      if (properties != null && properties.get("bcv.warn") != "false") {
        println("Librarian: BCV is only configured by default if using KGP 2.2+ (currently detected is '$kotlinPluginVersion'). Set bcv.warn=false in your librarian.root.properties file to remove this warning.")
      }
    }
  }
}

fun Project.configureBcv() {
  configureBcv(null)
}