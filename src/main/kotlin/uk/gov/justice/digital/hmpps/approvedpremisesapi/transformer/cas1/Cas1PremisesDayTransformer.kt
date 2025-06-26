package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import java.time.LocalDate

@Component
class Cas1PremisesDayTransformer {

  fun toCas1PremisesDaySummary(
    date: LocalDate,
    premisesCapacity: Cas1PremiseCapacityForDay,
    spaceBookings: List<Cas1SpaceBookingDaySummary>,
    outOfServiceBeds: List<Cas1OutOfServiceBedSummary>,
    spaceBookingSummaries: List<Cas1SpaceBookingSummary>,
  ) = Cas1PremisesDaySummary(
    forDate = date,
    previousDate = date.minusDays(1),
    nextDate = date.plusDays(1),
    capacity = premisesCapacity,
    spaceBookings = spaceBookings,
    outOfServiceBeds = outOfServiceBeds,
    spaceBookingSummaries = spaceBookingSummaries,
  )
}
