package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.countOverlappingDays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.overlaps
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormattedHourOfDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toWeekAndDayDurationString
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

  @Nested
  inner class ToWeekAndDayDurationString {

    @ParameterizedTest
    @CsvSource(
      "0, 0 days",
      "1, 1 day",
      "2, 2 days",
      "3, 3 days",
      "4, 4 days",
      "5, 5 days",
      "6, 6 days",
      "7, 1 week",
      "8, 1 week and 1 day",
      "9, 1 week and 2 days",
      "10, 1 week and 3 days",
      "11, 1 week and 4 days",
      "12, 1 week and 5 days",
      "13, 1 week and 6 days",
      "14, 2 weeks",
      "15, 2 weeks and 1 day",
      "23, 3 weeks and 2 days",
      "365, 52 weeks and 1 day",
    )
    fun `toWeekAndDayDurationString 0 days`(days: Int, expectedResult: String) {
      assertThat(toWeekAndDayDurationString(days)).isEqualTo(expectedResult)
    }
  }
}
