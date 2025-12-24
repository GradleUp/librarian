package com.gradleup.librarian.gradle

import gratatouille.GPlugin
import org.gradle.api.Project

/**
 * Unused plugin, only serves as a convenience to pull the librarian jar into the build classpath
 */
@GPlugin(id = "com.gradleup.librarian")
fun plugin(project: Project) {}
