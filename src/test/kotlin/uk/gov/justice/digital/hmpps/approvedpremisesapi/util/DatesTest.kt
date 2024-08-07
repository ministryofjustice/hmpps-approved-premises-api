package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class DatesTest {

  @Nested
  inner class Instant {

    @SuppressWarnings("UnusedPrivateProperty")
    @Test
    fun minusRandomSeconds() {
      val now = LocalDateTime.of(2020, 1, 1, 12, 30, 0)

      for (i in 1..1000) {
        val result = now.toInstant(ZoneOffset.UTC).minusRandomSeconds(60 * 30)
        assertThat(result.toLocalDateTime()).isAfter(LocalDateTime.of(2020, 1, 1, 11, 29, 59))
        assertThat(result.toLocalDateTime()).isBefore(LocalDateTime.of(2020, 1, 1, 12, 30, 1))
      }
    }
  }
}
