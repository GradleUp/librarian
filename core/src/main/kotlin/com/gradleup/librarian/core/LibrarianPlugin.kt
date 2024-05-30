package com.gradleup.librarian.core

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Unused plugin, only serves as a convenience to pull the librarian jar into the build classpath
 */
class LibrarianPlugin: Plugin<Project> {
    override fun apply(target: Project) {
    }
}