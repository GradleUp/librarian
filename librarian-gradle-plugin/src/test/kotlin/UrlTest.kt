import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URLEncoder
import kotlin.test.Test
import kotlin.test.assertEquals

class UrlTest {
  @Test
  fun test() {
    val name = "m2/com/gradleup/compat/patrouille/compat-patrouille-gradle-plugin/0.0.1-SNAPSHOT/maven-metadata.xml"

    val httpUrl = "https://example.com".toHttpUrl().newBuilder()
      .addPathSegment(name)
      .build()
      .toString()

    val usingUrlEncode = "https://example.com/" + URLEncoder.encode(name, "UTF-8")
    assertEquals(httpUrl, usingUrlEncode)
  }
}