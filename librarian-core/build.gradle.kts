import com.gradleup.librarian.core.librarianModule

plugins {
    id("org.jetbrains.kotlin.jvm")
}

librarianModule()

dependencies {
    implementation(libs.coroutines)

    testImplementation(kotlin("test"))
}

