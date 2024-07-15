import com.gradleup.librarian.core.tooling.toVersionOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SemVerTest {
  @Test
  fun test() {
    "1.2.3-alpha.0".toVersionOrNull().apply {
      assertNotNull(this)
      assertEquals(1, major)
      assertEquals(2, minor)
      assertEquals(3, patch)
      assertEquals(false, isSnapshot)
      assertEquals("alpha", preRelease?.name)
      assertEquals(0, preRelease?.version)
    }
    "1.2.3".toVersionOrNull().apply {
      assertNotNull(this)
      assertEquals(1, major)
      assertEquals(2, minor)
      assertEquals(3, patch)
      assertEquals(false, isSnapshot)
      assertEquals(null, preRelease)
    }
    "1.2.3-SNAPSHOT".toVersionOrNull().apply {
      assertNotNull(this)
      assertEquals(1, major)
      assertEquals(2, minor)
      assertEquals(3, patch)
      assertEquals(true, isSnapshot)
      assertEquals(null, preRelease)
    }
    "1.2.3-alpha.0-SNAPSHOT".toVersionOrNull().apply {
      assertNotNull(this)
      assertEquals(1, major)
      assertEquals(2, minor)
      assertEquals(3, patch)
      assertEquals(true, isSnapshot)
      assertEquals("alpha", preRelease?.name)
      assertEquals(0, preRelease?.version)
    }
  }
}