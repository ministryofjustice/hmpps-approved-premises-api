package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3TurnaroundTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.LocalDate

@Component
class BookingTransformer(
  private val personTransformer: PersonTransformer,
  private val cas3ArrivalTransformer: Cas3ArrivalTransformer,
  private val departureTransformer: DepartureTransformer,
  private val nonArrivalTransformer: NonArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val cas3ConfirmationTransformer: Cas3ConfirmationTransformer,
  private val extensionTransformer: ExtensionTransformer,
  private val bedTransformer: BedTransformer,
  private val cas3TurnaroundTransformer: Cas3TurnaroundTransformer,
  private val enumConverterFactory: EnumConverterFactory,
  private val workingDayService: WorkingDayService,
) {

  fun transformJpaToApi(jpa: BookingEntity, personInfo: PersonInfoResult): Booking {
    val hasNonZeroDayTurnaround = jpa.turnaround != null && jpa.turnaround!!.workingDayCount != 0

    return Booking(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      arrivalDate = jpa.arrivalDate,
      departureDate = jpa.departureDate,
      serviceName = enumConverterFactory.getConverter(ServiceName::class.java).convert(jpa.service) ?: throw InternalServerErrorProblem("Could not convert '${jpa.service}' to a ServiceName"),
      status = determineStatus(jpa),
      arrival = cas3ArrivalTransformer.transformJpaToArrival(jpa.arrival),
      departure = departureTransformer.transformJpaToApi(jpa.departure),
      departures = jpa.departures.map { departureTransformer.transformJpaToApi(it)!! },
      nonArrival = nonArrivalTransformer.transformJpaToApi(jpa.nonArrival),
      cancellation = cancellationTransformer.transformJpaToApi(jpa.cancellation),
      cancellations = jpa.cancellations.map { cancellationTransformer.transformJpaToApi(it)!! },
      confirmation = cas3ConfirmationTransformer.transformJpaToApi(jpa.confirmation),
      extensions = jpa.extensions.map(extensionTransformer::transformJpaToApi),
      bed = jpa.bed?.let { bedTransformer.transformJpaToApi(it) },
      originalArrivalDate = jpa.originalArrivalDate,
      originalDepartureDate = jpa.originalDepartureDate,
      createdAt = jpa.createdAt.toInstant(),
      turnaround = jpa.turnaround?.let(cas3TurnaroundTransformer::transformJpaToApi),
      turnarounds = jpa.turnarounds.map(cas3TurnaroundTransformer::transformJpaToApi),
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

  private fun determineApprovedPremisesStatus(jpa: BookingEntity) = when {
    jpa.nonArrival != null -> BookingStatus.notMinusArrived
    jpa.arrival != null && jpa.departure == null -> BookingStatus.arrived
    jpa.departure != null -> BookingStatus.departed
    jpa.cancellation != null -> BookingStatus.cancelled
    jpa.arrival == null && jpa.nonArrival == null -> BookingStatus.awaitingMinusArrival
    else -> throw RuntimeException("Could not determine status for Booking ${jpa.id}")
  }

  private fun determineTemporaryAccommodationStatus(jpa: BookingEntity): BookingStatus = determineTemporaryAccommodationStatus(
    hasTurnaround = jpa.turnaround != null,
    turnaroundWorkingDayCount = jpa.turnaround?.workingDayCount,
    departureDate = jpa.departureDate,
    hasCancellation = jpa.cancellation != null,
    hasDeparture = jpa.departure != null,
    hasArrival = jpa.arrival != null,
    hasNonArrival = jpa.nonArrival != null,
    hasConfirmation = jpa.confirmation != null,
  )

  fun determineTemporaryAccommodationStatus(
    hasTurnaround: Boolean,
    turnaroundWorkingDayCount: Int?,
    departureDate: LocalDate,
    hasCancellation: Boolean,
    hasDeparture: Boolean,
    hasArrival: Boolean,
    hasNonArrival: Boolean,
    hasConfirmation: Boolean,
  ): BookingStatus {
    val (hasNonZeroDayTurnaround, turnaroundPeriodEnded) = getTurnaroundData(hasTurnaround, turnaroundWorkingDayCount, departureDate)
    val hasZeroDayTurnaround = !hasTurnaround || turnaroundWorkingDayCount == 0
    return when {
      hasCancellation -> BookingStatus.cancelled
      hasDeparture && hasNonZeroDayTurnaround && !turnaroundPeriodEnded -> BookingStatus.departed
      hasDeparture && (turnaroundPeriodEnded || hasZeroDayTurnaround) -> BookingStatus.closed
      hasArrival -> BookingStatus.arrived
      hasNonArrival -> BookingStatus.notMinusArrived
      hasConfirmation -> BookingStatus.confirmed
      else -> BookingStatus.provisional
    }
  }

  private fun getTurnaroundData(
    hasTurnaround: Boolean,
    turnaroundWorkingDayCount: Int?,
    departureDate: LocalDate,
  ): Pair<Boolean, Boolean> {
    val hasNonZeroDayTurnaround = hasTurnaround && turnaroundWorkingDayCount != null && turnaroundWorkingDayCount != 0
    val turnaroundPeriodEnded = getTurnaroundPeriodEnded(
      hasNonZeroDayTurnaround,
      departureDate,
      turnaroundWorkingDayCount,
    )
    return hasNonZeroDayTurnaround to turnaroundPeriodEnded
  }

  private fun getTurnaroundPeriodEnded(
    hasNonZeroDayTurnaround: Boolean,
    departureDate: LocalDate,
    turnaroundWorkingDayCount: Int?,
  ) = if (!hasNonZeroDayTurnaround) {
    false
  } else {
    workingDayService.addWorkingDays(departureDate, turnaroundWorkingDayCount!!).isBefore(LocalDate.now())
  }
}
