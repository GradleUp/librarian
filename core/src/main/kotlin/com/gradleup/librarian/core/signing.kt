package com.gradleup.librarian.core

import com.gradleup.librarian.core.internal.findEnvironmentVariable
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

fun Project.configureSigning(signing: Signing) {
  val publishing = extensions.findByName("publishing") as PublishingExtension?
  require(publishing != null) {
    "Librarian: You need to configure publishing before configure signing"
  }
  configureSigning(publishing, signing)
}

internal fun Project.configureSigning(publishing: PublishingExtension, signing: Signing) {
  plugins.apply("signing")
  val signingExtension = extensions.getByType(SigningExtension::class.java)

  signingExtension.useInMemoryPgpKeys(
      signing.privateKey,
      signing.privateKeyPassword
  )
  signingExtension.sign(publishing.publications)

  tasks.withType(Sign::class.java).configureEach {
    it.isEnabled = signing.privateKey != null
  }

  // See https://github.com/gradle/gradle/issues/26091
  tasks.withType(AbstractPublishToMaven::class.java).configureEach {
    val signingTasks = tasks.withType(Sign::class.java)
    it.mustRunAfter(signingTasks)
  }

  // https://github.com/gradle/gradle/issues/26132
  afterEvaluate {
    tasks.all {
      if (it.name.startsWith("compileTestKotlin")) {
        val target = it.name.substring("compileTestKotlin".length)
        val sign = try {
          tasks.named("sign${target}Publication")
        } catch (e: Throwable) {
          null
        }
        if (sign != null) {
          it.dependsOn(sign)
        }
      }
    }
  }
}

class Signing(
    val privateKey: String?,
    val privateKeyPassword: String?,
) {
  class Builder internal constructor(internal val project: Project) {
    /**
     * The armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----
     * Get it with gpg --armour --export-secret-keys KEY_ID
     */
    var privateKey: String? = null

    /**
     * The password for [privateKey]
     */
    var privateKeyPassword: String? = null

    fun fromEnvironmentVariables() {
      privateKey = project.findEnvironmentVariable("LIBRARIAN_SIGNING_PRIVATE_KEY")
      privateKeyPassword = project.findEnvironmentVariable("LIBRARIAN_SIGNING_PRIVATE_KEY_PASSWORD")
    }

    fun build(): Signing {
      return Signing(
          privateKey = privateKey,
          privateKeyPassword = privateKeyPassword
      )
    }
  }
}
