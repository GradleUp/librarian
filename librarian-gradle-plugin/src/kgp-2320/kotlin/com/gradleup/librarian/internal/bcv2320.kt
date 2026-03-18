@file:OptIn(ExperimentalAbiValidation::class)

package com.gradleup.librarian.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

fun Project.configureBcv2320(
  excludePatterns: List<String> = emptyList(),
) {
  extensions.getByType(KotlinProjectExtension::class.java).apply {
    val abiValidation = this.extensions.findByName("abiValidation") ?: error("No abiValidation extension found")

    when (abiValidation) {
      is AbiValidationExtension -> {
        abiValidation.enabled.set(true)
        abiValidation.filters {
          it.exclude.byNames.addAll(excludePatterns)
        }
      }

      is AbiValidationMultiplatformExtension -> {
        abiValidation.enabled.set(true)
        abiValidation.filters {
          it.exclude.byNames.addAll(excludePatterns)
        }
      }

      else -> error("Librarian: unknown abiValidation extension type: '${abiValidation.javaClass.name}'")
    }

    /**
     * Compatibility tasks to not break the brain muscle
     */
    tasks.register("apiDump") {
      it.dependsOn("updateKotlinAbi")
      it.doLast {
        println("`apiDump` is deprecated. Use `updateKotlinAbi` instead")
      }
    }
    tasks.register("apiCheck") {
      it.dependsOn("checkKotlinAbi")
      it.doLast {
        println("`apiCheck` is deprecated. Use `updateKotlinAbi` instead")
      }
    }
  }
}