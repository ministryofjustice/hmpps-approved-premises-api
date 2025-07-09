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
    .map { bedSummary ->
      BedDayState(
        bed = bedSummary,
        day = day,
        inactiveReason = bedSummary.getInactiveReason(day, outOfServiceBedRecordsToConsider),
      )
    }

  fun spaceBookingsForDay(
    day: LocalDate,
    spaceBookingsToConsider: List<Cas1SpaceBookingEntity>,
  ): List<Cas1SpaceBookingEntity> = spaceBookingsToConsider
    .filter { it.isExpectedOrResident(day) }

  private fun Cas1PlanningBedSummary.getInactiveReason(day: LocalDate, outOfServiceBedRecords: List<OutOfServiceBedSummary>): BedInactiveReason? {
    val outOfServiceRecord = this.findOutOfServiceRecord(day, outOfServiceBedRecords)
    return if (outOfServiceRecord != null) {
      BedOutOfService(outOfServiceRecord.getReasonName())
    } else if (!BedEntity.isActive(day, this.bedEndDate)) {
      BedEnded(this.bedEndDate!!)
    } else {
      null
    }
  }

  private fun Cas1PlanningBedSummary.findOutOfServiceRecord(
    day: LocalDate,
    outOfServiceBedRecords: List<OutOfServiceBedSummary>,
  ) = outOfServiceBedRecords.firstOrNull {
    it.getBedId() == this.bedId &&
      !day.isBefore(it.getStartDate()) &&
      !day.isAfter(it.getEndDate())
  }
}
