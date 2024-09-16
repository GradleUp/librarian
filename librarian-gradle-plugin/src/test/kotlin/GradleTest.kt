import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.MultipartBody
import com.apollographql.mockserver.enqueueString
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartReader
import okio.Buffer
import okio.use
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class GradleTest {
  @Ignore // Blocked on https://github.com/apollographql/apollo-kotlin-mockserver/pull/10
  @Test
  fun portalEndToEndTest() = runBlocking {
    val mockWebServer = MockServer()
    mockWebServer.enqueueString("ok")

    withTestProject("simple") { projectDir ->

      projectDir.resolve("librarian.properties")
          .replace(Regex("sonatype.baseUrl=.*"), "sonatype.baseUrl=http://127.0.0.1:${mockWebServer.port()}")

      GradleRunner.create()
          .withProjectDir(projectDir)
          .withArguments("librarianPublishToMavenCentral", "--stacktrace")
          .forwardOutput()
          .withEnvironment(mapOf(
              "LIBRARIAN_SONATYPE_USERNAME" to "fake_user",
              "LIBRARIAN_SONATYPE_PASSWORD" to "fake_password"
          )
          )
          .run()
          .apply {
            assertEquals(TaskOutcome.SUCCESS, task(":librarianDeployToPortal")?.outcome)
          }

      mockWebServer.takeRequest().apply {
          /*
           * XXX: better parsing
           */
          val boundary = headers.get("Content-Type")?.substringAfterLast("boundary=") ?: error("no Content-Type")
        MultipartReader(Buffer().write(body), boundary = boundary).use {
          it.nextPart()!!.use {
            ZipInputStream(it.body.inputStream()).use {
              var entry = it.nextEntry
              while (entry != null) {
                println("Got ${entry.name}")
                entry = it.nextEntry
              }
            }
          }
        }
      }
    }
  }
}

private suspend fun withTestProject(name: String, block: suspend (File) -> Unit) {
  val dest = File("build/testProject")
  dest.deleteRecursively()

  File("testProjects/$name").copyRecursively(dest)

  block(dest)
}

fun File.replace(regex: Regex, replacement: String) {
  writeText(readText().replace(regex, replacement))
}