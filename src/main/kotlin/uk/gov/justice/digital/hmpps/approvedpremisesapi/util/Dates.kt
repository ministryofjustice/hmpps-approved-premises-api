package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun LocalDate.getDaysUntilInclusive(end: LocalDate): List<LocalDate> {
  val result = mutableListOf<LocalDate>()

  var currentDate = this
  while (currentDate <= end) {
    result += currentDate
    currentDate = currentDate.plusDays(1)
  }

  return result
}

fun LocalDate.getDaysUntilExclusiveEnd(end: LocalDate): List<LocalDate> {
  val result = mutableListOf<LocalDate>()

  var currentDate = this
  while (currentDate < end) {
    result += currentDate
    currentDate = currentDate.plusDays(1)
  }

  return result
}

fun LocalDate.toLocalDateTime(zoneOffset: ZoneOffset = ZoneOffset.UTC) = OffsetDateTime.of(this, LocalTime.MIN, zoneOffset)

infix fun ClosedRange<LocalDate>.overlaps(other: ClosedRange<LocalDate>): Boolean {
  /*
  false:  <--A-->             (A entirely before B)
                    <--B-->

  false:            <--A-->   (A entirely after B)
          <--B-->

  true:   <--A-->             (A ends after B begins)
             <--B-->

  true:      <--A-->          (A starts before B ends)
          <--B-->

  true:   <-------A------->   (A fully contains B)
            <--B-->

  true:        <--A-->        (B fully contains A)
          <-------B------->
  */

  val thisFullyBefore = this.start < other.start && this.endInclusive < other.start
  val thisFullyAfter = this.endInclusive > other.endInclusive && this.start > other.endInclusive

  return !(thisFullyBefore || thisFullyAfter)
}
