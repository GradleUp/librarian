package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.hasAndroid
import com.gradleup.librarian.core.internal.hasKotlin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

fun Project.configureJavaCompatibility(
    javaVersion: JavaVersion,
) {
    configureJavaCompatibilityInternal(javaVersion)
}

fun Project.configureJavaCompatibility(
    javaVersion: Int,
) {
    configureJavaCompatibilityInternal(javaVersion.toJavaVersion())
}

fun Project.configureAndroidCompatibility(
    minSdk: Int,
    compileSdk: Int,
    testTargetSdk: Int
) {
    if (hasAndroid) {
        configureAndroidVersionInternal(minSdk, compileSdk, testTargetSdk)
    }
}

fun Project.configureKotlinCompatibility(
    version: String
) {
    val kotlin = kotlinExtensionOrNull
    if (kotlin == null) {
        return
    }
    val kotlinVersion = KotlinVersion.fromVersion(version.substringBeforeLast("."))
    kotlin.forEachCompilerOptions {
        apiVersion.set(kotlinVersion)
        languageVersion.set(kotlinVersion)
    }

    kotlin.coreLibrariesVersion = version
}

internal fun Project.configureJavaCompatibilityInternal(javaVersion: JavaVersion) {
    tasks.withType(JavaCompile::class.java) {
        it.options.release.set(javaVersion.majorVersion.toInt())
    }
    if (hasAndroid) {
        androidJavaVersion(javaVersion)
    }
    configureKotlinJvmTarget(javaVersion)
}


internal fun Int.toJavaVersion(): JavaVersion {
    return JavaVersion.forClassVersion(this + 44)
}

internal fun JavaVersion.toInt(): Int {
    return majorVersion.toInt()
}
