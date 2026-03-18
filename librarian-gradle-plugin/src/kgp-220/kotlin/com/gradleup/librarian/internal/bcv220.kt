@file:OptIn(ExperimentalAbiValidation::class)

package com.gradleup.librarian.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

fun Project.configureBcv220(
  excludePatterns: List<String> = emptyList(),
) {
  extensions.getByType(KotlinProjectExtension::class.java).apply {
    val abiValidation = this.extensions.findByName("abiValidation") ?: error("No abiValidation extension found")

    when (abiValidation) {
      is AbiValidationExtension -> {
        abiValidation.enabled.set(true)
        abiValidation.filters {
          it.excluded.byNames.addAll(excludePatterns)
        }
      }

      is AbiValidationMultiplatformExtension -> {
        abiValidation.enabled.set(true)
        abiValidation.filters {
          it.excluded.byNames.addAll(excludePatterns)
        }
      }

      else -> error("Librarian: unknown abiValidation extension type: '${abiValidation.javaClass.name}'")
    }

    /**
     * Compatibility tasks to not break the brain muscle
     */
    tasks.named("build") {
      it.dependsOn("checkLegacyAbi")
    }
    tasks.register("apiDump") {
      it.dependsOn("updateLegacyAbi")
    }
    tasks.register("apiCheck") {
      it.dependsOn("checkLegacyAbi")
    }
  }
}