import com.gradleup.librarian.core.librarianModule

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
}

librarianModule()

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("net.mbonnin.vespene:vespene-lib:0.5")
    implementation("com.gradleup.maven-sympathy:maven-sympathy:0.0.2")
    api("dev.adamko.dokkatoo:dokkatoo-plugin:2.3.1")
    api("org.jetbrains.kotlinx:binary-compatibility-validator:0.15.0-Beta.2")

    compileOnly("dev.gradleplugins:gradle-api:8.0")
    compileOnly("com.android.tools.build:gradle:8.2.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")

    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        create("com.gradleup.librarian") {
            id = "com.gradleup.librarian"
            description =
                "The librarian plugin. The plugin is no-op and merely serves for plugin the librarian .jar into the build classpath"
            implementationClass = "com.gradleup.librarian.core.LibrarianPlugin"
        }
    }
}
