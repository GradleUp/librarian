package com.gradleup.librarian.gradle

import com.gradleup.librarian.gradle.internal.hasAndroid
import org.gradle.api.Project


fun Project.configureAndroidCompatibility(
    minSdk: Int,
    compileSdk: Int,
    testTargetSdk: Int
) {
    if (hasAndroid) {
        configureAndroidVersionInternal(minSdk, compileSdk, testTargetSdk)
    }
}

