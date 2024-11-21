package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.time.LocalDate
import java.util.stream.Stream

data class DateRange(
  val fromInclusive: LocalDate,
  val toInclusive: LocalDate,
) {
  fun orderedDatesInRange(): Stream<LocalDate> = fromInclusive.datesUntil(toInclusive.plusDays(1))
}
