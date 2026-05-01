import com.gradleup.librarian.core.tooling.semVerOrNull
import com.gradleup.librarian.core.tooling.semVerOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemVerTest {
  @Test
  fun test() {
    "1.2.3-alpha.0".semVerOrNull().apply {
      assertNotNull(this)
      assertEquals(1, major)
      assertEquals(2, minor)
      assertEquals(3, patch)
      assertEquals(false, isSnapshot)
      assertEquals("alpha", preRelease?.name)
      assertEquals(0, preRelease?.version)
    }
    "1.2.3".semVerOrNull().apply {
      assertNotNull(this)
      assertEquals(1, major)
      assertEquals(2, minor)
      assertEquals(3, patch)
      assertEquals(false, isSnapshot)
      assertEquals(null, preRelease)
    }
    "1.2.3-SNAPSHOT".semVerOrNull().apply {
      assertNotNull(this)
      assertEquals(1, major)
      assertEquals(2, minor)
      assertEquals(3, patch)
      assertEquals(true, isSnapshot)
      assertEquals(null, preRelease)
    }
    "1.2.3-alpha.0-SNAPSHOT".semVerOrNull().apply {
      assertNotNull(this)
      assertEquals(1, major)
      assertEquals(2, minor)
      assertEquals(3, patch)
      assertEquals(true, isSnapshot)
      assertEquals("alpha", preRelease?.name)
      assertEquals(0, preRelease?.version)
    }
    "2.4.0-Beta2".semVerOrNull().apply {
      assertNotNull(this)
      assertEquals(2, major)
      assertEquals(4, minor)
      assertEquals(0, patch)
      assertEquals(false, isSnapshot)
      assertEquals("Beta2", preRelease?.name)
      assertEquals(null, preRelease?.version)
    }
    assertEquals("2.4.0-Beta2", "2.4.0-Beta2".semVerOrThrow().toString())
    assertTrue("2.4.0-Beta2".semVerOrThrow() < "2.4.0-Beta2.1".semVerOrThrow())
  }
}
