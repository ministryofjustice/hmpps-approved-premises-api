package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesDaySummary
import java.time.LocalDate

@Component
class Cas1PremisesDayTransformer {

  fun toCas1PremisesDaySummary(
    date: LocalDate,
    premisesCapacity: Cas1PremiseCapacity,
  ) =
    Cas1PremisesDaySummary(
      forDate = date,
      previousDate = date.minusDays(1),
      nextDate = date.plusDays(1),
      capacity = premisesCapacity.capacity.first(),
      spaceBookings = emptyList(),
      outOfServiceBeds = emptyList(),
    )
}
