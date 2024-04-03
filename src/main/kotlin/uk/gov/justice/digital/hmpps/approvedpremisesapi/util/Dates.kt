package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val cas1UiExtendedDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")
val cas2UiExtendedDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM y")
val cas1UiTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ha")
val cas2UiTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mma")
val cas2ApplicationDataDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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

infix fun ClosedRange<LocalDate>.countOverlappingDays(other: ClosedRange<LocalDate>): Int {
  val latestStart = maxOf(this.start, other.start)
  val earliestEnd = minOf(this.endInclusive, other.endInclusive)
  val days = ChronoUnit.DAYS.between(latestStart, earliestEnd).toInt() + 1
  return if (days > 0) days else 0
}

fun LocalDate.toUiFormat(): String = this.format(cas1UiExtendedDateFormat)

fun LocalDate.toCas2UiFormat(): String = this.format(cas2UiExtendedDateFormat)

fun LocalDate.toUtcOffsetDateTime(): OffsetDateTime = OffsetDateTime.of(
  this,
  LocalTime.of(0, 0, 0, 0),
  ZoneOffset.UTC,
)

fun OffsetDateTime.toUiFormattedHourOfDay(): String = this.format(cas1UiTimeFormat).lowercase()

fun OffsetDateTime.toCas2UiFormattedHourOfDay(): String = this.format(cas2UiTimeFormat).lowercase()

/*
  takes OffsetDateTime and returns string in "yyyy-mm-dd" format
 */
fun OffsetDateTime.toCas2ApplicationDataFormattedDate(): String = this.format(cas2ApplicationDataDateFormat).lowercase()

fun earliestDateOf(date1: LocalDate, date2: LocalDate): LocalDate {
  if (date1.isBefore(date2)) return date1

  return date2
}

fun latestDateOf(date1: LocalDate, date2: LocalDate): LocalDate {
  if (date1.isAfter(date2)) return date1

  return date2
}
