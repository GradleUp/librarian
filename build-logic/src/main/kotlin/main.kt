import com.gradleup.librarian.Compatibility
import com.gradleup.librarian.RepositoryMetadata
import com.gradleup.librarian.Sonatype
import com.gradleup.librarian.SonatypeHost
import org.gradle.api.Project

fun Project.repositoryMetadata() = RepositoryMetadata {
  this.groupId = "com.gradleup.librarian"
  this.vcsUrl = "https://github.com/gradleup/librarian"
  this.license = "MIT License"
  this.developers = "GradleUp team"
  this.description = "The buddy for your libraries"
  this.version = "0.0.0"
  this.licenseUrl = "https://github.com/gradleup/librarian/LICENSE"
}

fun Project.sonatype(): Sonatype? {
  if (providers.environmentVariable("OSSRH_USER").orNull == null) {
    return null
  }
  return Sonatype {
    this.host = SonatypeHost.Default
    this.username = providers.environmentVariable("OSSRH_USER").orNull
    this.password = providers.environmentVariable("OSSRH_PASSWORD").orNull
    this.stagingProfile = providers.environmentVariable("OSSRH_STAGING_PROFILE").orNull
    this.autoRelease = false
  }
}

fun Project.compatibility() = Compatibility {
  this.jdkRelease = 11
}
