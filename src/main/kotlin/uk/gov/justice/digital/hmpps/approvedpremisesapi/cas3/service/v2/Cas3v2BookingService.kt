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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DepartureRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ExtensionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.getPersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.serviceScopeMatches
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
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
  private val cas3DepartureRepository: Cas3DepartureRepository,
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val cas3v2TurnaroundRepository: Cas3v2TurnaroundRepository,
  private val assessmentRepository: AssessmentRepository,
  private val offenderService: OffenderService,
  private val cas3DomainEventService: Cas3v2DomainEventService,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val cas3v2ConfirmationRepository: Cas3v2ConfirmationRepository,
  private val cas3ExtensionRepository: Cas3ExtensionRepository,
  private val workingDayService: WorkingDayService,
  private val userAccessService: UserAccessService,
  private val assessmentService: AssessmentService,
  private val featureFlagService: FeatureFlagService,
) {
  companion object {
    const val ARRIVAL_AFTER_LATEST_DATE_LIMIT_DAYS: Long = 14
  }

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
    enableTurnarounds: Boolean,
  ): CasResult<Cas3BookingEntity> = validatedCasResult {
    if (bedspaceId == null) {
      "$.bedspaceId" hasValidationError "empty"
      return@validatedCasResult fieldValidationError
    }

    val expectedLastUnavailableDate =
      workingDayService.addWorkingDays(departureDate, premises.turnaroundWorkingDays)
    getBookingWithConflictingDates(arrivalDate, closedDate = expectedLastUnavailableDate, bookingId = null, bedspaceId)?.let {
      return@validatedCasResult it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate()} which overlaps with the desired dates"
    }

    getVoidBedspaceWithConflictingDates(
      startDate = arrivalDate,
      endDate = expectedLastUnavailableDate,
      bookingId = null,
      bedspaceId = bedspaceId,
    )?.let {
      return@validatedCasResult it.id hasConflictError "A Void Bedspace already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
    }

    cas3BedspaceRepository.findArchivedBedspaceByBedspaceIdAndDate(bedspaceId, departureDate)?.let {
      return@validatedCasResult it.id hasConflictError "BedSpace is archived from ${it.endDate} which overlaps with the desired dates"
    }

    val bedspace = cas3BedspaceRepository.findByIdOrNull(bedspaceId)
    if (bedspace == null) {
      "$.bedspaceId" hasValidationError "doesNotExist"
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
        workingDayCount = getWorkingDayCount(enableTurnarounds, premises),
        createdAt = bookingCreatedAt,
        booking = booking,
      ),
    )

    booking.turnarounds += turnaround

    cas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(booking, user)

    success(booking)
  }

  private fun getWorkingDayCount(enableTurnarounds: Boolean, premises: Cas3PremisesEntity) = when (enableTurnarounds) {
    true -> premises.turnaroundWorkingDays
    else -> 0
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

    if (arrivalDate.isBefore(LocalDate.now().minusDays(ARRIVAL_AFTER_LATEST_DATE_LIMIT_DAYS))) {
      "$.arrivalDate" hasValidationError "arrivalAfterLatestDate"
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

  fun createDeparture(
    booking: Cas3BookingEntity,
    dateTime: OffsetDateTime,
    reasonId: UUID,
    moveOnCategoryId: UUID,
    notes: String?,
    user: UserEntity,
  ) = validatedCasResult<Cas3DepartureEntity> {
    if (booking.arrivalDate.toLocalDateTime().isAfter(dateTime)) {
      "$.dateTime" hasValidationError "beforeBookingArrivalDate"
    }

    if (featureFlagService.getBooleanFlag("cas3-validate-booking-departure-in-future") && dateTime.isAfter(OffsetDateTime.now())) {
      validationErrors["$.dateTime"] = "departureDateInFuture"
    }

    val reason = departureReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      "$.reasonId" hasValidationError "doesNotExist"
    } else if (!reason.serviceScopeMatches(booking.service)) {
      "$.reasonId" hasValidationError "incorrectDepartureReasonServiceScope"
    }

    val moveOnCategory = moveOnCategoryRepository.findByIdOrNull(moveOnCategoryId)
    if (moveOnCategory == null) {
      "$.moveOnCategoryId" hasValidationError "doesNotExist"
    } else if (!moveOnCategory.serviceScopeMatches(booking.service)) {
      "$.moveOnCategoryId" hasValidationError "incorrectMoveOnCategoryServiceScope"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val isFirstDeparture = booking.departures.isEmpty()

    val departureEntity = cas3DepartureRepository.save(
      Cas3DepartureEntity(
        id = UUID.randomUUID(),
        dateTime = dateTime,
        reason = reason!!,
        moveOnCategory = moveOnCategory!!,
        destinationProvider = null,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )
    booking.status = Cas3BookingStatus.departed
    booking.departureDate = dateTime.toLocalDate()
    updateBooking(booking)
    booking.departures += departureEntity

    when (isFirstDeparture) {
      true -> cas3DomainEventService.savePersonDepartedEvent(booking, user)
      else -> cas3DomainEventService.savePersonDepartureUpdatedEvent(booking, user)
    }

    return CasResult.Success(departureEntity)
  }

  @Transactional
  fun createConfirmation(
    booking: Cas3BookingEntity,
    dateTime: OffsetDateTime,
    notes: String?,
    user: UserEntity,
  ) = validatedCasResult<Cas3v2ConfirmationEntity> {
    if (booking.confirmation != null) {
      return CasResult.GeneralValidationError<Cas3v2ConfirmationEntity>("This Booking already has a Confirmation set")
    }

    val cas3ConfirmationEntity = cas3v2ConfirmationRepository.save(
      Cas3v2ConfirmationEntity(
        id = UUID.randomUUID(),
        dateTime = dateTime,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )
    booking.status = Cas3BookingStatus.confirmed
    updateBooking(booking)
    booking.confirmation = cas3ConfirmationEntity

    cas3DomainEventService.saveBookingConfirmedEvent(booking, user)
    findAndCloseAssessment(booking, user)

    return CasResult.Success(cas3ConfirmationEntity)
  }

  @Transactional
  fun createExtension(
    booking: Cas3BookingEntity,
    newDepartureDate: LocalDate,
    notes: String?,
  ) = validatedCasResult<Cas3ExtensionEntity> {
    val expectedLastUnavailableDate =
      workingDayService.addWorkingDays(newDepartureDate, booking.turnaround?.workingDayCount ?: 0)

    getBookingWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, booking.id, booking.bedspace.id)?.let {
      return@validatedCasResult it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate()} which overlaps with the desired dates"
    }

    getVoidBedspaceWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, null, booking.bedspace.id)?.let {
      return@validatedCasResult it.id hasConflictError "A Void Bedspace already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
    }

    if (booking.arrivalDate.isAfter(newDepartureDate)) {
      return "$.newDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }

    val extensionEntity = Cas3ExtensionEntity(
      id = UUID.randomUUID(),
      previousDepartureDate = booking.departureDate,
      newDepartureDate = newDepartureDate,
      notes = notes,
      booking = booking,
      createdAt = OffsetDateTime.now(),
    )

    val extension = cas3ExtensionRepository.save(extensionEntity)
    booking.departureDate = extensionEntity.newDepartureDate
    booking.extensions.add(extension)
    updateBooking(booking)

    return CasResult.Success(extensionEntity)
  }

  private fun findAndCloseAssessment(booking: Cas3BookingEntity, user: UserEntity) {
    booking.application?.let {
      val assessment =
        assessmentRepository.findByApplicationIdAndReallocatedAtNull(booking.application!!.id)
      if (assessment != null) {
        closeAssessment(assessment.id, user, booking)
      }
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun closeAssessment(
    assessmentId: UUID,
    user: UserEntity,
    booking: Cas3BookingEntity,
  ) {
    try {
      extractEntityFromCasResult(assessmentService.closeAssessment(user, assessmentId))
    } catch (exception: Exception) {
      log.error("Unable to close CAS3 assessment $assessmentId for booking ${booking.id} ", exception)
      Sentry.captureException(RuntimeException("Unable to close CAS3 assessment $assessmentId for booking ${booking.id} ", exception))
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

  fun throwIfBookingDatesConflict(
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    thisEntityId: UUID?,
    bedspaceId: UUID,
  ) {
    getBookingWithConflictingDates(arrivalDate, departureDate, thisEntityId, bedspaceId)?.let {
      throw ConflictProblem(
        it.id,
        "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates",
      )
    }
  }

  fun throwIfVoidBedspaceDatesConflict(
    startDate: LocalDate,
    endDate: LocalDate,
    bookingId: UUID?,
    bedspaceId: UUID,
  ) {
    getVoidBedspaceWithConflictingDates(startDate, endDate, bookingId, bedspaceId)?.let {
      throw ConflictProblem(
        it.id,
        "A Void Bedspace already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates",
      )
    }
  }

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
