package com.gradleup.librarian.gradle

import org.gradle.api.Project
import java.util.*

fun Project.librarianProperties(): Properties {
    return rootProject.file("librarian.properties").let {
        require (it.exists()) {
            "No librarian.properties found at ${it.absolutePath}"
        }
        Properties().apply {
            it.inputStream().use {
                load(it)
            }
        }
    }
}
