package com.gradleup.librarian.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

@OptIn(ExperimentalAbiValidation::class)
fun Project.configureBcv(warnIfMissing: Boolean = true, block: (variantSpec: Any) -> Unit = {}) {
  extensions.getByType(KotlinProjectExtension::class.java).apply {
    val abiValidation = this.extensions.findByName("abiValidation")

    if (abiValidation != null) {
      when (abiValidation) {
        is AbiValidationExtension -> {
          abiValidation.enabled.set(true)
          abiValidation.variants.configureEach {
            block(it)
          }
        }
        is AbiValidationMultiplatformExtension -> {
          abiValidation.enabled.set(true)
          abiValidation.variants.configureEach {
            block(it)
          }
        }
         else -> error("Librarian: unknown abiValidation extension type: '${abiValidation.javaClass.name}'")
      }

      tasks.named("build") {
        it.dependsOn("checkLegacyAbi")
      }
      /**
       * Compatibility tasks to not break the brain muscle
       */
      tasks.register("apiDump") {
        it.dependsOn("updateLegacyAbi")
      }
      tasks.register("apiCheck") {
        it.dependsOn("checkLegacyAbi")
      }
    } else {
      if (warnIfMissing) {
        println("Librarian: BCV is only configured by default if using KGP 2.2+ (currently detected is '${getKotlinPluginVersion()}'). Set bcv.warn=false in your librarian.root.properties file to remove this warning.")
      }
    }
  }
}
