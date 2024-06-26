import com.gradleup.librarian.core.librarianModule
import org.gradle.api.internal.artifacts.dependencies.DefaultFileCollectionDependency

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
}

librarianModule()

dependencies {
    implementation(libs.coroutines)
    implementation(libs.vespene)
    implementation(libs.maven.sympathy)
    api(libs.dokkatoo)
    api(libs.bcv)

    compileOnly(libs.gradle.api)
    compileOnly(libs.agp)
    compileOnly(libs.kgp)

    testImplementation(kotlin("test"))
}

configurations.getByName("api").dependencies.removeIf {
    it is DefaultFileCollectionDependency
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
