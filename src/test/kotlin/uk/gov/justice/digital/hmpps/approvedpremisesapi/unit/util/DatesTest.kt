package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.overlaps
import java.time.LocalDate

class DatesTest {
  @Test
  fun `overlaps returns false if one range is fully before another`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2000-12-31")
    val b = LocalDate.parse("2010-01-01")..LocalDate.parse("2010-12-31")

    assertThat(a overlaps b).isFalse
    assertThat(b overlaps a).isFalse
  }

  @Test
  fun `overlaps returns true if one range ends after another begins`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2010-12-31")
    val b = LocalDate.parse("2010-01-01")..LocalDate.parse("2020-12-31")

    assertThat(a overlaps b).isTrue
    assertThat(b overlaps a).isTrue
  }

  @Test
  fun `overlaps returns true if one range fully contains another`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2020-12-31")
    val b = LocalDate.parse("2010-01-01")..LocalDate.parse("2010-12-31")

    assertThat(a overlaps b).isTrue
    assertThat(b overlaps a).isTrue
  }

  @Test
  fun `overlaps considers ranges that share a single day to overlap`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2001-01-01")
    val b = LocalDate.parse("2001-01-01")..LocalDate.parse("2002-01-01")

    assertThat(a overlaps b).isTrue
    assertThat(b overlaps a).isTrue
  }
}
