package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.LocalDate

@Component
class BookingTransformer(
  private val workingDayService: WorkingDayService,
) {

  fun determineStatus(jpa: BookingEntity) = when {
    jpa.service == ServiceName.approvedPremises.value -> determineApprovedPremisesStatus(jpa)
    jpa.service == ServiceName.temporaryAccommodation.value -> determineTemporaryAccommodationStatus(jpa)
    else -> throw RuntimeException("Could not determine service for Booking ${jpa.id}")
  }

  private fun determineApprovedPremisesStatus(jpa: BookingEntity) = when {
    jpa.nonArrival != null -> BookingStatus.notMinusArrived
    jpa.arrival != null && jpa.departure == null -> BookingStatus.arrived
    jpa.departure != null -> BookingStatus.departed
    jpa.cancellation != null -> BookingStatus.cancelled
    jpa.arrival == null && jpa.nonArrival == null -> BookingStatus.awaitingMinusArrival
    else -> throw RuntimeException("Could not determine status for Booking ${jpa.id}")
  }

  private fun determineTemporaryAccommodationStatus(jpa: BookingEntity): BookingStatus {
    val hasNonZeroDayTurnaround = jpa.turnaround != null && jpa.turnaround!!.workingDayCount != 0
    val hasZeroDayTurnaround = jpa.turnaround == null || jpa.turnaround!!.workingDayCount == 0
    val turnaroundPeriodEnded = if (!hasNonZeroDayTurnaround) {
      false
    } else {
      workingDayService.addWorkingDays(jpa.departureDate, jpa.turnaround!!.workingDayCount).isBefore(LocalDate.now())
    }

    return when {
      jpa.cancellation != null -> BookingStatus.cancelled
      jpa.departure != null && hasNonZeroDayTurnaround && !turnaroundPeriodEnded -> BookingStatus.departed
      jpa.departure != null && (turnaroundPeriodEnded || hasZeroDayTurnaround) -> BookingStatus.closed
      jpa.arrival != null -> BookingStatus.arrived
      jpa.nonArrival != null -> BookingStatus.notMinusArrived
      jpa.confirmation != null -> BookingStatus.confirmed
      else -> BookingStatus.provisional
    }
  }
}
