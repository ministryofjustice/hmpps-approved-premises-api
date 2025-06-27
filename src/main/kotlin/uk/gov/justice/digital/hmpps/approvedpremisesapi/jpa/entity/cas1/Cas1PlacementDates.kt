package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import java.time.LocalDate

data class Cas1PlacementDates(
  val expectedArrival: LocalDate,
  val duration: Int,
) {
  fun expectedDeparture(): LocalDate = expectedArrival.plusDays(duration.toLong())

  fun toApiType(): PlacementDates = PlacementDates(expectedArrival, duration)
}
