package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1NationalOccupancy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1NationalOccupancyPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PremiseCapacitySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacityForDay

@Service
class Cas1NationalOccupancyTransformer(val searchResultsTransformer: Cas1SpaceSearchResultsTransformer) {

  fun toCapacitySummary(
    capacities: Cas1PremisesService.Cas1PremisesCapacities,
    requestedRoomCharacteristics: Set<Cas1SpaceCharacteristic>,
    premisesSummaries: List<CandidatePremises>,
  ): Cas1NationalOccupancy = Cas1NationalOccupancy(
    startDate = capacities.startDate,
    endDate = capacities.endDate,
    premises = premisesSummaries.map {
      toPremisesCapacity(
        premisesSummary = it,
        capacities = capacities,
        requestedRoomCharacteristics = requestedRoomCharacteristics,
      )
    },
  )

  private fun toPremisesCapacity(
    premisesSummary: CandidatePremises,
    capacities: Cas1PremisesService.Cas1PremisesCapacities,
    requestedRoomCharacteristics: Set<Cas1SpaceCharacteristic>,
  ): Cas1NationalOccupancyPremises {
    val capacity = capacities.results.first { it.premisesId == premisesSummary.premisesId }

    return Cas1NationalOccupancyPremises(
      summary = searchResultsTransformer.toPremisesSearchResultSummary(premisesSummary),
      distanceInMiles = premisesSummary.distanceInMiles?.toBigDecimal(),
      capacity = capacity.byDay.map {
        toCas1PremiseCapacitySummary(
          capacityForDay = it,
          requestedRoomCharacteristics = requestedRoomCharacteristics,
        )
      },
    )
  }

  private fun toCas1PremiseCapacitySummary(
    capacityForDay: PremiseCapacityForDay,
    requestedRoomCharacteristics: Set<Cas1SpaceCharacteristic>,
  ): Cas1PremiseCapacitySummary {
    val capacityExcludingCharacteristics = Cas1PremiseCapacitySummary(
      date = capacityForDay.day,
      forRoomCharacteristic = null,
      inServiceBedCount = capacityForDay.availableBedCount,
      vacantBedCount = capacityForDay.availableBedCount - capacityForDay.bookingCount,
    )

    if (requestedRoomCharacteristics.isEmpty()) {
      return capacityExcludingCharacteristics
    } else {
      val lowestCapacityForCharacteristics = capacityForDay
        .characteristicAvailability
        .filter { characteristic -> requestedRoomCharacteristics.any { it.value == characteristic.characteristicPropertyName } }
        .minBy { it.availableBedCount - it.bookingCount }
        .let {
          Cas1PremiseCapacitySummary(
            date = capacityForDay.day,
            forRoomCharacteristic = Cas1SpaceCharacteristic.valueOf(it.characteristicPropertyName),
            inServiceBedCount = it.availableBedCount,
            vacantBedCount = it.availableBedCount - it.bookingCount,
          )
        }

      return if (lowestCapacityForCharacteristics.vacantBedCount < capacityExcludingCharacteristics.vacantBedCount) {
        lowestCapacityForCharacteristics
      } else {
        capacityExcludingCharacteristics
      }
    }
  }
}
