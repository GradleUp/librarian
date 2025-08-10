package com.gradleup.librarian.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal val Project.kotlinExtensionOrNull: KotlinProjectExtension? get() = extensions.findByName("kotlin") as KotlinProjectExtension?

val Project.kotlinExtension: KotlinProjectExtension get() = kotlinExtensionOrNull ?: error("no 'kotlin' extension found")
