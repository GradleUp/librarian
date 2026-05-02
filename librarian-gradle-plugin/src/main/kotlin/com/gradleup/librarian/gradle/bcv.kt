@file:OptIn(ExperimentalAbiValidation::class)

package com.gradleup.librarian.gradle

import com.gradleup.librarian.core.tooling.semVerOrThrow
import com.gradleup.librarian.internal.configureBcv220
import com.gradleup.librarian.internal.configureBcv2320
import com.gradleup.librarian.internal.configureBcv240
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

fun Project.configureBcv(
  warnIfMissing: Boolean = true,
  excludePatterns: List<String> = emptyList(),
) {
  // See https://kotlinlang.org/docs/whatsnew2320.html#improvements-to-binary-compatibility-validation-in-kgp
  val kgpVersion = this@configureBcv.getKotlinPluginVersion()

  when {
    kgpVersion >= "2.4.0" -> configureBcv240(excludePatterns)
    kgpVersion >= "2.3.20" -> configureBcv2320(excludePatterns)
    kgpVersion >= "2.2.0" -> configureBcv220(excludePatterns)
    else -> {
      if (warnIfMissing) {
        println("Librarian: BCV is only configured by default if using KGP 2.2+ (currently detected is '${getKotlinPluginVersion()}'). Set bcv.warn=false in your librarian.root.properties file to remove this warning.")
      }
    }
  }
}
