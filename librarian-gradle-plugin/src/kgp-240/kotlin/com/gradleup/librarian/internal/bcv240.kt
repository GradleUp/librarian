@file:OptIn(ExperimentalAbiValidation::class)

package com.gradleup.librarian.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

fun Project.configureBcv240(
  excludePatterns: List<String> = emptyList(),
) {
  extensions.getByType(KotlinProjectExtension::class.java).apply {
    abiValidation {
      it.filters {
        it.exclude.byNames.addAll(excludePatterns)
      }
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