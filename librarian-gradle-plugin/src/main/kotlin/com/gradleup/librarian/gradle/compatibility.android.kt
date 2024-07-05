package com.gradleup.librarian.gradle

import com.android.build.api.dsl.LibraryExtension
import com.gradleup.librarian.gradle.internal.androidExtension
import org.gradle.api.Incubating
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal fun Project.androidJavaVersion(javaVersion: JavaVersion) {
    androidExtension.compileOptions.apply {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

@Incubating
internal fun Project.configureAndroidVersionInternal(minSdk: Int, compileSdk: Int, testTargetSdk: Int) {
    configureAndroidVersionInternal(minSdk, compileSdk)
    androidExtension.apply {
        if (this is LibraryExtension) {
            @Suppress("UnstableApiUsage")
            testOptions.targetSdk = testTargetSdk
        }
    }
}

internal fun Project.configureAndroidVersionInternal(minSdk: Int, compileSdk: Int) {
    androidExtension.apply {
        this.compileSdk = compileSdk
        this.defaultConfig {
            this.minSdk = minSdk
        }
    }
}