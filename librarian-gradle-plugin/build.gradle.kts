import com.gradleup.librarian.gradle.librarianModule
import org.gradle.api.internal.artifacts.dependencies.DefaultFileCollectionDependency

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
}

librarianModule()

dependencies {
    api(libs.bcv)
    api(libs.dokkatoo)

    implementation(project(":librarian-core"))
    implementation(libs.coroutines)
    implementation(libs.vespene)
    implementation(libs.maven.sympathy)
    implementation(libs.okhttp)

    compileOnly(libs.gradle.api)
    compileOnly(libs.agp)
    compileOnly(libs.kgp)

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
    testImplementation(libs.mockserver)
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
            implementationClass = "com.gradleup.librarian.gradle.LibrarianPlugin"
        }
    }
}
