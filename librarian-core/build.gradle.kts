import com.gradleup.librarian.core.librarianModule

plugins {
    id("org.jetbrains.kotlin.jvm")
}

librarianModule()

dependencies {
    implementation(libs.coroutines)
    implementation(libs.jsonpath)
    implementation(libs.jsonpathkt)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)

    testImplementation(kotlin("test"))
}

