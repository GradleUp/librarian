@file:OptIn(ExperimentalAbiValidation::class)

package com.gradleup.librarian.gradle

import com.gradleup.librarian.core.tooling.semVerOrThrow
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

fun Project.configureBcv(
  warnIfMissing: Boolean = true,
  excludePatterns: List<String> = emptyList(),
) {
  extensions.getByType(KotlinProjectExtension::class.java).apply {
    val abiValidation = this.extensions.findByName("abiValidation")

    if (abiValidation != null) {
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

      // See https://kotlinlang.org/docs/whatsnew2320.html#improvements-to-binary-compatibility-validation-in-kgp
      val kgpVersion = this@configureBcv.getKotlinPluginVersion().semVerOrThrow()

      /**
       * Compatibility tasks to not break the brain muscle
       */
      if (kgpVersion >= "2.3.20".semVerOrThrow()) {
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
      } else {
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
    } else {
      if (warnIfMissing) {
        println("Librarian: BCV is only configured by default if using KGP 2.2+ (currently detected is '${getKotlinPluginVersion()}'). Set bcv.warn=false in your librarian.root.properties file to remove this warning.")
      }
    }
  }
}
