package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3ConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3TurnaroundTransformer
import java.time.LocalDate

@Component
class BookingTransformer(
  private val personTransformer: PersonTransformer,
  private val arrivalTransformer: ArrivalTransformer,
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
      // key worker is a legacy CAS1 only field that is no longer populated. This will be removed once migration to space bookings is complete
      keyWorker = null,
      status = determineStatus(jpa),
      arrival = arrivalTransformer.transformJpaToApi(jpa.arrival),
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

  fun transformJpaToApi(jpa: Cas3BookingEntity, personInfo: PersonInfoResult): Booking {
    val hasNonZeroDayTurnaround = jpa.turnaround != null && jpa.turnaround!!.workingDayCount != 0

    return Booking(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      arrivalDate = jpa.arrivalDate,
      departureDate = jpa.departureDate,
      serviceName = enumConverterFactory.getConverter(ServiceName::class.java).convert(jpa.service) ?: throw InternalServerErrorProblem("Could not convert '${jpa.service}' to a ServiceName"),
      // key worker is a legacy CAS1 only field that is no longer populated. This will be removed once migration to space bookings is complete
      keyWorker = null,
      status = determineStatus(
        jpa.turnaround,
        jpa.departureDate,
        jpa.cancellation,
        jpa.departure,
        jpa.arrival,
        jpa.nonArrival,
        jpa.confirmation,
      ),
      arrival = arrivalTransformer.transformJpaToApi(jpa.arrival),
      departure = departureTransformer.transformJpaToApi(jpa.departure),
      departures = jpa.departures.map { departureTransformer.transformJpaToApi(it)!! },
      nonArrival = nonArrivalTransformer.transformJpaToApi(jpa.nonArrival),
      cancellation = cancellationTransformer.transformJpaToApi(jpa.cancellation),
      cancellations = jpa.cancellations.map { cancellationTransformer.transformJpaToApi(it)!! },
      confirmation = cas3ConfirmationTransformer.transformJpaToApi(jpa.confirmation),
      extensions = jpa.extensions.map(extensionTransformer::transformJpaToApi),
      bed = jpa.bedspace?.let { bedTransformer.transformJpaToApi(it) },
      originalArrivalDate = jpa.originalArrivalDate,
      originalDepartureDate = jpa.originalDepartureDate,
      createdAt = jpa.createdAt.toInstant(),
      turnaround = jpa.turnaround?.let(cas3TurnaroundTransformer::transformJpaToApi),
      turnarounds = jpa.turnarounds.map(cas3TurnaroundTransformer::transformJpaToApi),
      turnaroundStartDate = if (hasNonZeroDayTurnaround) workingDayService.addWorkingDays(jpa.departureDate, 1) else null,
      effectiveEndDate = if (hasNonZeroDayTurnaround) workingDayService.addWorkingDays(jpa.departureDate, jpa.turnaround!!.workingDayCount) else jpa.departureDate,
      applicationId = jpa.application?.id,
      assessmentId = jpa.application?.getLatestAssessment()?.id,
      premises = jpa.cas3Premises.let { BookingPremisesSummary(it.id, it.name) },
    )
  }

  fun determineStatus(jpa: BookingEntity) = when {
    jpa.service == ServiceName.approvedPremises.value -> determineApprovedPremisesStatus(jpa)
    jpa.service == ServiceName.temporaryAccommodation.value -> determineTemporaryAccommodationStatus(
      jpa.turnaround,
      jpa.departureDate,
      jpa.cancellation,
      jpa.departure,
      jpa.arrival,
      jpa.nonArrival,
      jpa.confirmation,
    )
    else -> throw RuntimeException("Could not determine service for Booking ${jpa.id}")
  }

  fun determineStatus(
    turnaround: Cas3TurnaroundEntity?,
    departureDate: LocalDate,
    cancellation: CancellationEntity?,
    departure: DepartureEntity?,
    arrival: ArrivalEntity?,
    nonArrival: NonArrivalEntity?,
    confirmation: Cas3ConfirmationEntity?
  ) = determineTemporaryAccommodationStatus(
    turnaround,
    departureDate,
    cancellation,
    departure,
    arrival,
    nonArrival,
    confirmation,
  )

  fun transformToWithdrawable(jpa: BookingEntity) = Withdrawable(
    jpa.id,
    WithdrawableType.booking,
    listOf(DatePeriod(jpa.arrivalDate, jpa.departureDate)),
  )

  private fun determineApprovedPremisesStatus(jpa: BookingEntity) = when {
    jpa.nonArrival != null -> BookingStatus.notMinusArrived
    jpa.arrival != null && jpa.departure == null -> BookingStatus.arrived
    jpa.departure != null -> BookingStatus.departed
    jpa.cancellation != null -> BookingStatus.cancelled
    jpa.arrival == null && jpa.nonArrival == null -> BookingStatus.awaitingMinusArrival
    else -> throw RuntimeException("Could not determine status for Booking ${jpa.id}")
  }

  private fun determineTemporaryAccommodationStatus(
    turnaround: Cas3TurnaroundEntity?,
    departureDate: LocalDate,
    cancellation: CancellationEntity?,
    departure: DepartureEntity?,
    arrival: ArrivalEntity?,
    nonArrival: NonArrivalEntity?,
    confirmation: Cas3ConfirmationEntity?,
  ): BookingStatus {
    val hasNonZeroDayTurnaround = turnaround != null && turnaround.workingDayCount != 0
    val hasZeroDayTurnaround = turnaround == null || turnaround.workingDayCount == 0
    val turnaroundPeriodEnded = if (!hasNonZeroDayTurnaround) {
      false
    } else {
      workingDayService.addWorkingDays(departureDate, turnaround!!.workingDayCount).isBefore(LocalDate.now())
    }

    return when {
      cancellation != null -> BookingStatus.cancelled
      departure != null && hasNonZeroDayTurnaround && !turnaroundPeriodEnded -> BookingStatus.departed
      departure != null && (turnaroundPeriodEnded || hasZeroDayTurnaround) -> BookingStatus.closed
      arrival != null -> BookingStatus.arrived
      nonArrival != null -> BookingStatus.notMinusArrived
      confirmation != null -> BookingStatus.confirmed
      else -> BookingStatus.provisional
    }
  }
}
