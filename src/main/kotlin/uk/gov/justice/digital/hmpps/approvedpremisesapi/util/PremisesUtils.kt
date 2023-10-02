package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import java.time.LocalDate

fun getDateCapacities(premises: PremisesEntity, premisesService: PremisesService): List<DateCapacity> {
  val lastBookingDate = premisesService.getLastBookingDate(premises)
  val lastLostBedsDate = premisesService.getLastLostBedsDate(premises)

  val capacityForPeriod = premisesService.getAvailabilityForRange(
    premises,
    LocalDate.now(),
    maxOf(
      LocalDate.now(),
      lastBookingDate ?: LocalDate.now(),
      lastLostBedsDate ?: LocalDate.now(),
    ),
  )

  return capacityForPeriod.map {
    DateCapacity(
      date = it.key,
      availableBeds = it.value.getFreeCapacity(premises.totalBeds),
    )
  }
}
