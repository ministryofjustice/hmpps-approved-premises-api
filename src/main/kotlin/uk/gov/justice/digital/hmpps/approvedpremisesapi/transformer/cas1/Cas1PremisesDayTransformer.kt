package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import java.time.LocalDate

@Component
class Cas1PremisesDayTransformer {

  fun toCas1PremisesDaySummary(
    date: LocalDate,
    outOfServiceBeds: List<Cas1OutOfServiceBedSummary>,
    spaceBookingSummaries: List<Cas1SpaceBookingSummary>,
  ) = Cas1PremisesDaySummary(
    forDate = date,
    previousDate = date.minusDays(1),
    nextDate = date.plusDays(1),
    outOfServiceBeds = outOfServiceBeds,
    spaceBookingSummaries = spaceBookingSummaries,
  )
}
