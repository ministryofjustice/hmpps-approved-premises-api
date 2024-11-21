package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacitySummary

@Component
class Cas1PremiseCapacitySummaryTransformer {

  fun toCas1PremiseCapacitySummary(
    premiseCapacity: PremiseCapacitySummary,
  ) = Cas1PremiseCapacity(
    premisesId = premiseCapacity.premise.id,
    startDate = premiseCapacity.range.fromInclusive,
    endDate = premiseCapacity.range.toInclusive,
    capacity = premiseCapacity.byDay.map { it.toApiType() },
  )

  private fun SpacePlanningService.PremiseCapacityForDay.toApiType() = Cas1PremiseCapacityForDay(
    totalBedCount = totalBedCount,
    availableBedCount = availableBedCount,
    bookingCount = bookingCount,
    characteristicAvailability = characteristicAvailability.map { it.toApiType() },
  )

  private fun SpacePlanningService.PremiseCharacteristicAvailability.toApiType() =
    Cas1PremiseCharacteristicAvailability(
      characteristic = Cas1SpaceCharacteristic.entries.first { it.value == this.characteristicPropertyName },
      availableBedsCount = availableBedsCount,
      bookingsCount = bookingsCount,
    )
}
