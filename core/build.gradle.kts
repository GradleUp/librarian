plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
}

group = rootProject.group
version = "0.0.2"

publishing {
    publications.configureEach {
        this as MavenPublication
        if (!name.lowercase().contains("marker")) {
            artifact(tasks.register("librarianEmptyJavadoc", org.gradle.jvm.tasks.Jar::class.java) {
                archiveClassifier.set("javadoc")
            })
            artifact(tasks.register("librarianEmptySources", org.gradle.jvm.tasks.Jar::class.java) {
                archiveClassifier.set("sources")
            })
        }

        pom {
            name.set("librarian")
            description.set("librarian")

            val githubUrl = "https://github.com/gradleup/librarian"

            url.set(githubUrl)

            scm {
                url.set(githubUrl)
                connection.set(githubUrl)
                developerConnection.set(githubUrl)
            }

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/GradleUp/librarian/blob/main/LICENSE")
                }
            }

            developers {
                developer {
                    id.set("GradleUp authors")
                    name.set("GradleUp authors")
                }
            }
        }
    }
    repositories {
        maven {
            name = "ossStaging"
            setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USER")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(System.getenv("GPG_KEY"), System.getenv("GPG_KEY_PASSWORD"))
    sign(publishing.publications)
}
tasks.withType(Sign::class.java).configureEach {
    isEnabled = System.getenv("GPG_KEY") != null
}

// See https://github.com/gradle/gradle/issues/26091
tasks.withType(AbstractPublishToMaven::class.java).configureEach {
    val signingTasks = tasks.withType(Sign::class.java)
    mustRunAfter(signingTasks)
}

// https://github.com/gradle/gradle/issues/26132
afterEvaluate {
    tasks.all {
        if (name.startsWith("compileTestKotlin")) {
            val target = name.substring("compileTestKotlin".length)
            val sign = try {
                tasks.named("sign${target}Publication")
            } catch (e: Throwable) {
                null
            }
            if (sign != null) {
                dependsOn(sign)
            }
        }
    }
}

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
