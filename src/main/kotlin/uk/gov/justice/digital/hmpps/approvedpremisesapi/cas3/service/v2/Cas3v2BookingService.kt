package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import io.sentry.Sentry
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.getPersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.serviceScopeMatches
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3v2BookingService(
  private val cas3BookingRepository: Cas3v2BookingRepository,
  private val cas3BedspaceRepository: Cas3BedspacesRepository,
  private val cas3CancellationRepository: Cas3CancellationRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val cas3ArrivalRepository: Cas3ArrivalRepository,
  private val cas3v2TurnaroundRepository: Cas3v2TurnaroundRepository,
  private val assessmentRepository: AssessmentRepository,
  private val offenderService: OffenderService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val workingDayService: WorkingDayService,
  private val userAccessService: UserAccessService,
  private val assessmentService: AssessmentService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getBooking(bookingId: UUID, premisesId: UUID?, user: UserEntity): CasResult<Cas3BookingEntity> {
    val booking = cas3BookingRepository.findByIdOrNull(bookingId)
      ?: return CasResult.NotFound("Booking", bookingId.toString())
    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      return CasResult.Unauthorised()
    } else if (premisesId != null && premisesId != booking.premises.id) {
      return CasResult.GeneralValidationError("The supplied premisesId does not match the booking's premises")
    }
    return CasResult.Success(booking)
  }

  @Transactional
  fun createBooking(
    user: UserEntity,
    premises: Cas3PremisesEntity,
    crn: String,
    nomsNumber: String?,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    bedspaceId: UUID?,
    assessmentId: UUID?,
  ): CasResult<Cas3BookingEntity> = validatedCasResult {
    if (bedspaceId == null) {
      "$.bedspaceId" hasValidationError "empty"
      return@validatedCasResult fieldValidationError
    }

    getBookingWithConflictingDates(arrivalDate, departureDate, bookingId = null, bedspaceId)?.let {
      return@validatedCasResult it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate()} which overlaps with the desired dates"
    }

    cas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
      bedspaceId,
      startDate = arrivalDate,
      endDate = departureDate,
    ).firstOrNull()?.let {
      return@validatedCasResult it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
    }

    cas3BedspaceRepository.findArchivedBedspaceByBedspaceIdAndDate(bedspaceId, departureDate)?.let {
      return@validatedCasResult it.id hasConflictError "BedSpace is archived from ${it.endDate} which overlaps with the desired dates"
    }

    val bedspace = cas3BedspaceRepository.findByIdOrNull(bedspaceId)
    if (bedspace == null) {
      "$.bedId" hasValidationError "doesNotExist"
    } else if (bedspace.startDate != null && bedspace.startDate!!.isAfter(arrivalDate)) {
      "$.arrivalDate" hasValidationError "bookingArrivalDateBeforeBedspaceStartDate"
    }

    if (departureDate.isBefore(arrivalDate)) {
      "$.departureDate" hasValidationError "beforeBookingArrivalDate"
    }

    val application = assessmentId?.let { id ->
      val result = assessmentRepository.findByIdOrNull(id)
      if (result == null) {
        "$.assessmentId" hasValidationError "doesNotExist"
      }
      result?.application
    }

    if (validationErrors.any()) {
      return@validatedCasResult fieldValidationError
    }

    val personResult = offenderService.getPersonSummaryInfoResult(crn, LaoStrategy.NeverRestricted)
    val offenderName = personResult.getPersonName()

    if (offenderName == null) {
      log.warn("Unable to get offender name for CRN $crn")
    }

    val bookingCreatedAt = OffsetDateTime.now()

    val booking = cas3BookingRepository.save(
      Cas3BookingEntity(
        id = UUID.randomUUID(),
        crn = crn,
        nomsNumber = nomsNumber,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
        arrivals = mutableListOf(),
        departures = mutableListOf(),
        nonArrival = null,
        cancellations = mutableListOf(),
        confirmation = null,
        extensions = mutableListOf(),
        dateChanges = mutableListOf(),
        premises = premises,
        bedspace = bedspace!!,
        service = ServiceName.temporaryAccommodation.value,
        originalArrivalDate = arrivalDate,
        originalDepartureDate = departureDate,
        createdAt = bookingCreatedAt,
        application = application,
        turnarounds = mutableListOf(),
        status = Cas3BookingStatus.provisional,
        offenderName = offenderName,
      ),
    )

    val turnaround = cas3v2TurnaroundRepository.save(
      Cas3v2TurnaroundEntity(
        id = UUID.randomUUID(),
        workingDayCount = 0,
        createdAt = bookingCreatedAt,
        booking = booking,
      ),
    )

    booking.turnarounds += turnaround

    cas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(booking, user)

    success(booking)
  }

  @Transactional
  fun createArrival(
    user: UserEntity,
    booking: Cas3BookingEntity,
    arrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    notes: String?,
  ): CasResult<Cas3ArrivalEntity> = validatedCasResult {
    if (expectedDepartureDate.isBefore(arrivalDate)) {
      "$.expectedDepartureDate" hasValidationError "beforeBookingArrivalDate"
      return@validatedCasResult fieldValidationError
    }
    val isFirstArrival = booking.arrivals.isEmpty()
    val arrivalEntity = cas3ArrivalRepository.save(
      Cas3ArrivalEntity(
        id = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalDateTime = arrivalDate.toLocalDateTime().toInstant(),
        expectedDepartureDate = expectedDepartureDate,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )

    booking.arrivalDate = arrivalDate
    booking.departureDate = expectedDepartureDate
    booking.status = Cas3BookingStatus.arrived
    updateBooking(booking)

    booking.arrivals += arrivalEntity

    when (isFirstArrival) {
      true -> cas3DomainEventService.savePersonArrivedEvent(booking, user)
      else -> cas3DomainEventService.savePersonArrivedUpdatedEvent(booking, user)
    }

    success(arrivalEntity)
  }

  @Transactional
  fun createCancellation(
    booking: Cas3BookingEntity,
    cancelledAt: LocalDate,
    reasonId: UUID,
    notes: String?,
    user: UserEntity,
  ): CasResult<Cas3CancellationEntity> = validatedCasResult {
    val reason = cancellationReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      "$.reason" hasValidationError "doesNotExist"
      return@validatedCasResult fieldValidationError
    } else if (!reason.serviceScopeMatches(booking.service)) {
      "$.reason" hasValidationError "incorrectCancellationReasonServiceScope"
      return@validatedCasResult fieldValidationError
    }

    val isFirstCancellations = booking.cancellations.isEmpty()
    val cancellationEntity = cas3CancellationRepository.save(
      Cas3CancellationEntity(
        id = UUID.randomUUID(),
        date = cancelledAt,
        reason = reason,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
        otherReason = null,
      ),
    )
    booking.status = Cas3BookingStatus.cancelled
    updateBooking(booking)
    booking.cancellations += cancellationEntity

    when (isFirstCancellations) {
      true -> cas3DomainEventService.saveBookingCancelledEvent(booking, user)
      else -> cas3DomainEventService.saveBookingCancelledUpdatedEvent(booking, user)
    }

    booking.application?.getLatestAssessment()?.let { assessmentEntity ->
      moveAssessmentToReadyToPlace(user, assessmentEntity, booking.id)
    }

    return success(cancellationEntity)
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun moveAssessmentToReadyToPlace(
    user: UserEntity,
    assessmentEntity: AssessmentEntity,
    bookingId: UUID,
  ) {
    try {
      extractEntityFromCasResult(
        assessmentService.acceptAssessment(
          user,
          assessmentEntity.id,
          assessmentEntity.document,
          null,
          null,
          null,
          "Automatically moved to ready-to-place after booking is cancelled",
        ),
      )
    } catch (exception: Exception) {
      log.error("Unable to move CAS3 assessment ${assessmentEntity.id} to ready-to-place queue for $bookingId ")
      Sentry.captureException(
        RuntimeException(
          "Unable to move CAS3 assessment ${assessmentEntity.id} for ready-to-place queue for $bookingId ",
          exception,
        ),
      )
    }
  }

  private fun updateBooking(bookingEntity: Cas3BookingEntity) = cas3BookingRepository.save(bookingEntity)

  fun getBookingWithConflictingDates(
    arrivalDate: LocalDate,
    closedDate: LocalDate,
    bookingId: UUID?,
    bedspaceId: UUID,
  ): Cas3BookingEntity? = cas3BookingRepository.findByBedspaceIdAndArrivingBeforeDate(bedspaceId, closedDate, excludeBookingId = bookingId)
    .firstOrNull { it.lastUnavailableDate() >= arrivalDate }

  fun getVoidBedspaceWithConflictingDates(
    startDate: LocalDate,
    endDate: LocalDate,
    bookingId: UUID?,
    bedspaceId: UUID,
  ) = cas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDateV2(
    bedspaceId,
    startDate,
    endDate,
    bookingId,
  ).firstOrNull()

  fun Cas3BookingEntity.lastUnavailableDate() = workingDayService.addWorkingDays(this.departureDate, this.turnaround?.workingDayCount ?: 0)
}
