package com.gradleup.librarian.cli

import java.io.File

internal fun File.guessLicense(): SupportedLicense? {
    listFiles()?.forEach {
        require(!it.name.startsWith("LICENSE.")) {
            "The LICENSE file must not have an extension (found '${it.path}'). Please rename your LICENSE file"
        }
    }

    resolve("LICENSE").apply {
        if (!exists()) {
            return null
        }
        useLines {
            it.take(5).forEach { line ->
                if (line.contains("MIT License")) {
                    return SupportedLicense.MIT
                }
            }
        }
    }

    error("Cannot guess license")
}