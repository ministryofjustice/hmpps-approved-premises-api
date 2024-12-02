package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PremisesBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.LocalDate

@Component
class BookingTransformer(
  private val personTransformer: PersonTransformer,
  private val arrivalTransformer: ArrivalTransformer,
  private val departureTransformer: DepartureTransformer,
  private val nonArrivalTransformer: NonArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val confirmationTransformer: ConfirmationTransformer,
  private val extensionTransformer: ExtensionTransformer,
  private val bedTransformer: BedTransformer,
  private val turnaroundTransformer: TurnaroundTransformer,
  private val enumConverterFactory: EnumConverterFactory,
  private val workingDayService: WorkingDayService,
) {

  fun transformBookingSummary(jpa: BookingSummary, personInfo: PersonSummaryInfoResult): PremisesBooking {
    return PremisesBooking(
      id = jpa.getID(),
      arrivalDate = jpa.getArrivalDate(),
      departureDate = jpa.getDepartureDate(),
      person = personTransformer.transformSummaryToPersonApi(personInfo),
      bed = jpa.getBedId()?.let {
        Bed(
          id = jpa.getBedId()!!,
          name = jpa.getBedName()!!,
          code = jpa.getBedCode(),
        )
      },
      status = jpa.getStatus(),
    )
  }

  fun transformJpaToApi(jpa: BookingEntity, personInfo: PersonInfoResult): Booking {
    val hasNonZeroDayTurnaround = jpa.turnaround != null && jpa.turnaround!!.workingDayCount != 0

    return Booking(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      arrivalDate = jpa.arrivalDate,
      departureDate = jpa.departureDate,
      serviceName = enumConverterFactory.getConverter(ServiceName::class.java).convert(jpa.service) ?: throw InternalServerErrorProblem("Could not convert '${jpa.service}' to a ServiceName"),
      // key worker is a legacy CAS1 only field that is no longer populated. This will be removed once migration to space bookings is complete
      keyWorker = null,
      status = determineStatus(jpa),
      arrival = arrivalTransformer.transformJpaToApi(jpa.arrival),
      departure = departureTransformer.transformJpaToApi(jpa.departure),
      departures = jpa.departures.map { departureTransformer.transformJpaToApi(it)!! },
      nonArrival = nonArrivalTransformer.transformJpaToApi(jpa.nonArrival),
      cancellation = cancellationTransformer.transformJpaToApi(jpa.cancellation),
      cancellations = jpa.cancellations.map { cancellationTransformer.transformJpaToApi(it)!! },
      confirmation = confirmationTransformer.transformJpaToApi(jpa.confirmation),
      extensions = jpa.extensions.map(extensionTransformer::transformJpaToApi),
      bed = jpa.bed?.let { bedTransformer.transformJpaToApi(it) },
      originalArrivalDate = jpa.originalArrivalDate,
      originalDepartureDate = jpa.originalDepartureDate,
      createdAt = jpa.createdAt.toInstant(),
      turnaround = jpa.turnaround?.let(turnaroundTransformer::transformJpaToApi),
      turnarounds = jpa.turnarounds.map(turnaroundTransformer::transformJpaToApi),
      turnaroundStartDate = if (hasNonZeroDayTurnaround) workingDayService.addWorkingDays(jpa.departureDate, 1) else null,
      effectiveEndDate = if (hasNonZeroDayTurnaround) workingDayService.addWorkingDays(jpa.departureDate, jpa.turnaround!!.workingDayCount) else jpa.departureDate,
      applicationId = jpa.application?.id,
      assessmentId = jpa.application?.getLatestAssessment()?.id,
      premises = jpa.premises.let { BookingPremisesSummary(it.id, it.name) },
    )
  }

  fun determineStatus(jpa: BookingEntity) = when {
    jpa.service == ServiceName.approvedPremises.value -> determineApprovedPremisesStatus(jpa)
    jpa.service == ServiceName.temporaryAccommodation.value -> determineTemporaryAccommodationStatus(jpa)
    else -> throw RuntimeException("Could not determine service for Booking ${jpa.id}")
  }

  fun transformToWithdrawable(jpa: BookingEntity) = Withdrawable(
    jpa.id,
    WithdrawableType.booking,
    listOf(DatePeriod(jpa.arrivalDate, jpa.departureDate)),
  )

  private fun determineApprovedPremisesStatus(jpa: BookingEntity) = when {
    jpa.nonArrival != null -> BookingStatus.notMinusArrived
    jpa.arrival != null && jpa.departure == null -> BookingStatus.ARRIVED
    jpa.departure != null -> BookingStatus.departed
    jpa.cancellation != null -> BookingStatus.CANCELLED
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
      jpa.cancellation != null -> BookingStatus.CANCELLED
      jpa.departure != null && hasNonZeroDayTurnaround && !turnaroundPeriodEnded -> BookingStatus.departed
      jpa.departure != null && (turnaroundPeriodEnded || hasZeroDayTurnaround) -> BookingStatus.closed
      jpa.arrival != null -> BookingStatus.ARRIVED
      jpa.nonArrival != null -> BookingStatus.notMinusArrived
      jpa.confirmation != null -> BookingStatus.CONFIRMED
      else -> BookingStatus.PROVISIONAL
    }
  }
}
