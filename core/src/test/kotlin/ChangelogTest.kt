import com.gradleup.librarian.core.tooling.changelogMd
import com.gradleup.librarian.core.tooling.extractChangelog
import com.gradleup.librarian.core.tooling.nextVersion
import com.gradleup.librarian.core.tooling.processChangelog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChangelogTest {
  @Test
  fun process() {
    processChangelog("""
      CHANGELOG!
      
      # Next version (unreleased)
      
      Bugfix

      # Version 0.0.1
      
      Initial release
    """.trimIndent().lineSequence(),
        "0.0.2"
    ).let {
      assertEquals("""
      CHANGELOG!
      
      # Next version (unreleased)

      PUT_CHANGELOG_HERE

      # Version 0.0.2
      _date_

      Bugfix

      # Version 0.0.1

      Initial release
      
      """.trimIndent(), it.replaceDate()
      )
    }
  }

  @Test
  fun missingUnreleased() {
    val result = kotlin.runCatching {
      processChangelog("""
      # Version 0.0.1
      
      Initial release!

    """.trimIndent().lineSequence(), "0.0.2"
      )
    }

    assertTrue(result.exceptionOrNull()?.message?.contains("The first H1 heading of $changelogMd must be '# $nextVersion'") == true)
  }

  @Test
  fun extract() {
    extractChangelog("""
      Changelog
      
      # Version 0.0.2
      
      Bugfixes
      
      # Version 0.0.1
      
      Initial release
    """.trimIndent().lineSequence(), "0.0.1"
    ).let {
      assertEquals("""
        
        Initial release
        
      """.trimIndent(),
          it
      )
    }
  }
}

private fun String.replaceDate(): String {
  return lines().mapIndexed { index: Int, s: String ->
    val regex = Regex("_[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}_")
    if (regex.matchEntire(s) != null) {
      "_date_"
    } else {
      s
    }
  }.joinToString("\n")
}
