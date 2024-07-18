import com.gradleup.librarian.core.librarianRoot

plugins {
  id("org.jetbrains.kotlin.jvm").version("2.0.0").apply(false)
  id("com.gradleup.librarian").version("0.0.2").apply(false)
}

librarianRoot()
