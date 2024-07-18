import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.gradleup.librarian.core.tooling.secretKeyRingOrNull
import com.gradleup.librarian.core.tooling.uploadKey
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class UploadKeyTest {
  @Test
  fun test(): Unit = runBlocking {
    val mockServer = MockServer()
    mockServer.enqueueString("OK")
    uploadKey(mockServer.url(), secretKeyRingOrNull(Path("/Users/mbonnin/git/apollo-kotlin/librarian_private_key.asc"))!!.publicKey())
    mockServer.takeRequest().apply {
      assertEquals("PUT", method)
    }
  }
}