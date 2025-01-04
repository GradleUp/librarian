import com.gradleup.librarian.gradle.Librarian

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
}

Librarian.module(project)

dependencies {
    implementation(project(":librarian-core"))
}

