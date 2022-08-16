package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.time.LocalDate

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
