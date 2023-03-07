package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import java.time.OffsetDateTime

fun withinSeconds(seconds: Long): Matcher<OffsetDateTime> {
  val matcher: Matcher<OffsetDateTime> = object : BaseMatcher<OffsetDateTime>() {
    private val now: OffsetDateTime = OffsetDateTime.now()

    override fun describeTo(description: Description?) {
      description?.appendText("within the last $seconds seconds (now: $now)")
    }

    override fun matches(actual: Any?): Boolean {
      val actualDateTime = when (actual) {
        is String -> OffsetDateTime.parse(actual)
        is OffsetDateTime -> actual
        else -> return false
      }

      if (now.isBefore(actualDateTime)) return false

      return actualDateTime.plusSeconds(seconds).isAfter(now)
    }
  }

  return matcher
}
