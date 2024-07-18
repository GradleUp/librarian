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
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pg)
    implementation(libs.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockserver)
}

