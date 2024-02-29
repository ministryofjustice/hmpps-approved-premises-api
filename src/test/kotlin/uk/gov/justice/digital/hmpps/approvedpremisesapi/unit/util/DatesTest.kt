package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.countOverlappingDays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.overlaps
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormattedHourOfDay
import java.time.LocalDate
import java.time.OffsetDateTime

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

  @Test
  fun `countOverlappingDays returns 0 if one range is fully before another`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2000-12-31")
    val b = LocalDate.parse("2010-01-01")..LocalDate.parse("2010-12-31")

    assertThat(a countOverlappingDays b).isZero()
    assertThat(b countOverlappingDays a).isZero()
  }

  @Test
  fun `countOverlappingDays returns the correct number of days if one range ends before another begins`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2010-12-31")
    val b = LocalDate.parse("2010-01-01")..LocalDate.parse("2020-12-31")

    assertThat(a countOverlappingDays b).isEqualTo(365)
    assertThat(b countOverlappingDays a).isEqualTo(365)
  }

  @Test
  fun `countOverlappingDays returns the correct number of days if both ranges are the same size`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2000-01-31")

    assertThat(a countOverlappingDays a).isEqualTo(31)
  }

  @Test
  fun `countOverlappingDays returns the range with smaller days if one range fully contains another`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2000-12-31")
    val b = LocalDate.parse("2000-06-01")..LocalDate.parse("2000-06-02")

    assertThat(a countOverlappingDays b).isEqualTo(2)
    assertThat(b countOverlappingDays a).isEqualTo(2)
  }

  @Test
  fun `countOverlappingDays considers ranges that share a single day to overlap`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2001-01-01")
    val b = LocalDate.parse("2001-01-01")..LocalDate.parse("2002-01-01")

    assertThat(a countOverlappingDays b).isOne()
    assertThat(b countOverlappingDays a).isOne()
  }

  @Test
  fun `countOverlappingDays returns one day if both ranges are the same single day`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2000-01-01")

    assertThat(a countOverlappingDays a).isOne()
  }

  @Test
  fun `countOverlappingDays returns 0 if ranges are adjacent but not overlapping`() {
    val a = LocalDate.parse("2000-01-01")..LocalDate.parse("2000-12-31")
    val b = LocalDate.parse("2001-01-01")..LocalDate.parse("2001-12-31")

    assertThat(a countOverlappingDays b).isZero()
    assertThat(b countOverlappingDays a).isZero()
  }

  @Test
  fun `toUiFormat formats a LocalDate correctly`() {
    val date = LocalDate.parse("2024-01-01")

    assertThat(date.toUiFormat()).isEqualTo("Monday 1 January 2024")
  }

  @Test
  fun `toUiFormattedHourOfDay formats a LocalDate correctly`() {
    val dateTime = OffsetDateTime.parse("2024-01-01T11:15:00Z")

    assertThat(dateTime.toUiFormattedHourOfDay()).isEqualTo("11am")
  }
}
