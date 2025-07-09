package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository.OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlanningBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import java.time.LocalDate

@Service
class SpacePlanningModelsFactory {

  fun allBedsDayState(
    day: LocalDate,
    beds: List<Cas1PlanningBedSummary>,
    outOfServiceBedRecordsToConsider: List<OutOfServiceBedSummary>,
  ): List<BedDayState> = beds
    .filter { BedEntity.isActive(day, it.bedEndDate) }
    .map { bedSummary ->
      BedDayState(
        bed = bedSummary,
        day = day,
        outOfService = bedSummary.findOutOfServiceRecord(day, outOfServiceBedRecordsToConsider) != null,
      )
    }

  fun spaceBookingsForDay(
    day: LocalDate,
    spaceBookingsToConsider: List<Cas1SpaceBookingEntity>,
  ): List<Cas1SpaceBookingEntity> = spaceBookingsToConsider
    .filter { it.isExpectedOrResident(day) }

  private fun Cas1PlanningBedSummary.findOutOfServiceRecord(
    day: LocalDate,
    outOfServiceBedRecords: List<OutOfServiceBedSummary>,
  ) = outOfServiceBedRecords.firstOrNull {
    it.getBedId() == this.bedId &&
      !day.isBefore(it.getStartDate()) &&
      !day.isAfter(it.getEndDate())
  }
}

data class BedDayState(
  val bed: Cas1PlanningBedSummary,
  val day: LocalDate,
  val outOfService: Boolean,
) {
  fun isActive() = !outOfService
}
