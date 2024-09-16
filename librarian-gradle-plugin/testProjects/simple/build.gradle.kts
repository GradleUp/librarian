import com.gradleup.librarian.gradle.librarianRoot

plugins {
    id("org.jetbrains.kotlin.jvm").version("2.0.0").apply(false)
    id("com.gradleup.librarian")
}

librarianRoot()
