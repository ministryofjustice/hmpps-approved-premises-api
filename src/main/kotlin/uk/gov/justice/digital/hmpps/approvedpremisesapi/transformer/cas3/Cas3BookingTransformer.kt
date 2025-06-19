package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.time.LocalDate

@Component
class Cas3BookingTransformer(
  private val personTransformer: PersonTransformer,
  private val arrivalTransformer: Cas3ArrivalTransformer,
  private val departureTransformer: Cas3DepartureTransformer,
  private val nonArrivalTransformer: Cas3NonArrivalTransformer,
  private val cancellationTransformer: Cas3CancellationTransformer,
  private val confirmationTransformer: Cas3ConfirmationTransformer,
  private val extensionTransformer: Cas3ExtensionTransformer,
  private val cas3BedspaceTransformer: Cas3BedspaceTransformer,
  private val cas3TurnaroundTransformer: Cas3TurnaroundTransformer,
  private val enumConverterFactory: EnumConverterFactory,
  private val workingDayService: WorkingDayService,
) {

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
      status = determineStatus(jpa),
      arrival = arrivalTransformer.transformJpaToApi(jpa.arrival),
      departure = departureTransformer.transformJpaToApi(jpa.departure),
      departures = jpa.departures.map { departureTransformer.transformJpaToApi(it)!! },
      nonArrival = nonArrivalTransformer.transformJpaToApi(jpa.nonArrival),
      cancellation = cancellationTransformer.transformJpaToApi(jpa.cancellation),
      cancellations = jpa.cancellations.map { cancellationTransformer.transformJpaToApi(it)!! },
      confirmation = confirmationTransformer.transformJpaToApi(jpa.confirmation),
      extensions = jpa.extensions.map(extensionTransformer::transformJpaToApi),
      bed = cas3BedspaceTransformer.transformJpaToApi(jpa.bedspace),
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

  fun transformToWithdrawable(jpa: Cas3BookingEntity) = Withdrawable(
    jpa.id,
    WithdrawableType.booking,
    listOf(DatePeriod(jpa.arrivalDate, jpa.departureDate)),
  )

  fun determineStatus(jpa: Cas3BookingEntity): BookingStatus {
    val (hasNonZeroDayTurnaround, hasZeroDayTurnaround, turnaroundPeriodEnded) = isTurnaroundPeriodEnded(jpa)
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

  private fun isTurnaroundPeriodEnded(jpa: Cas3BookingEntity): Triple<Boolean, Boolean, Boolean> {
    val hasNonZeroDayTurnaround = jpa.turnaround != null && jpa.turnaround!!.workingDayCount != 0
    val hasZeroDayTurnaround = jpa.turnaround == null || jpa.turnaround!!.workingDayCount == 0
    val turnaroundPeriodEnded = if (!hasNonZeroDayTurnaround) {
      false
    } else {
      workingDayService.addWorkingDays(jpa.departureDate, jpa.turnaround!!.workingDayCount).isBefore(LocalDate.now())
    }
    return Triple(hasNonZeroDayTurnaround, hasZeroDayTurnaround, turnaroundPeriodEnded)
  }
}
