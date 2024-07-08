package com.gradleup.librarian.gradle

import org.gradle.api.Project

abstract class LibrarianExtension(private val project: Project) {
    fun root() = project.librarianRoot()

    @JvmOverloads
    fun module(publish: Boolean = true) = project.librarianModule(publish)
}