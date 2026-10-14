package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import java.time.LocalDate

data class Cas1PlacementApplicationDates(
  val expectedArrival: LocalDate,
  val duration: Int?,
) {
  fun expectedDeparture(): LocalDate? = duration?.let { expectedArrival.plusDays(it.toLong()) }
}
