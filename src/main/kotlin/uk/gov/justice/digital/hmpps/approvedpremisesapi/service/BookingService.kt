package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_APP_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.serviceScopeMatches
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredBySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BookingService(
  private val premisesService: PremisesService,
  private val offenderService: OffenderService,
  private val workingDayService: WorkingDayService,
  private val bookingRepository: BookingRepository,
  private val arrivalRepository: ArrivalRepository,
  private val cancellationRepository: CancellationRepository,
  private val confirmationRepository: ConfirmationRepository,
  private val dateChangeRepository: DateChangeRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val bedRepository: BedRepository,
  private val placementRequestRepository: PlacementRequestRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val premisesRepository: PremisesRepository,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val cas1BookingEmailService: Cas1BookingEmailService,
  private val deliusService: DeliusService,
  private val cas1BookingDomainEventService: Cas1BookingDomainEventService,
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
) {
  fun updateBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)

  @SuppressWarnings("ThrowsCount")
  fun getBooking(id: UUID): AuthorisableActionResult<BookingAndPersons> {
    val booking = bookingRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound("Booking", id.toString())

    val user = userService.getUserForRequest()

    if (!userAccessService.userCanViewBooking(user, booking)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val personInfo = offenderService.getPersonInfoResult(booking.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    return AuthorisableActionResult.Success(BookingAndPersons(booking, personInfo))
  }

  data class BookingAndPersons(
    val booking: BookingEntity,
    val personInfo: PersonInfoResult,
  )

  @Transactional
  fun createApprovedPremisesBookingFromPlacementRequest(
    user: UserEntity,
    placementRequestId: UUID,
    bedId: UUID?,
    premisesId: UUID?,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return AuthorisableActionResult.NotFound("PlacementRequest", placementRequestId.toString())

    if (!user.hasPermission(UserPermission.CAS1_BOOKING_CREATE) && placementRequest.allocatedToUser?.id != user.id) {
      return AuthorisableActionResult.Unauthorised()
    }

    var bed: BedEntity?
    var premises: PremisesEntity?

    val validationResult = validated {
      if (placementRequest.isWithdrawn) {
        return@validated generalError("placementRequestIsWithdrawn")
      }

      val existingBooking = placementRequest.booking

      if (existingBooking != null && !existingBooking.isCancelled) {
        return@validated placementRequest.booking!!.id hasConflictError "A Booking has already been made for this Placement Request"
      }

      if (placementRequest.spaceBookings.any { it.isActive() }) {
        return@validated placementRequest.id hasConflictError "A Space Booking has already been made for this Placement Request"
      }

      if (departureDate.isBefore(arrivalDate)) {
        "$.departureDate" hasValidationError "beforeBookingArrivalDate"
      }

      if (bedId != null) {
        getBookingWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
          return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates"
        }

        getVoidBedspaceWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
          return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
        }

        bed = bedRepository.findByIdOrNull(bedId)

        if (bed == null) {
          "$.bedId" hasValidationError "doesNotExist"
        } else if (bed!!.room.premises !is ApprovedPremisesEntity) {
          "$.bedId" hasValidationError "mustBelongToApprovedPremises"
        }

        premises = bed!!.room.premises
      } else if (premisesId != null) {
        premises = premisesRepository.findByIdOrNull(premisesId)
        bed = null

        if (premises == null) {
          "$.premisesId" hasValidationError "doesNotExist"
        } else if (premises !is ApprovedPremisesEntity) {
          "$.premisesId" hasValidationError "mustBeAnApprovedPremises"
        }
      } else {
        return@validated generalError("You must identify the AP Area and Premises name")
      }

      if (validationErrors.any()) {
        return@validated fieldValidationError
      }

      val bookingCreatedAt = OffsetDateTime.now()

      val booking = bookingRepository.save(
        BookingEntity(
          id = UUID.randomUUID(),
          crn = placementRequest.application.crn,
          arrivalDate = arrivalDate,
          departureDate = departureDate,
          keyWorkerStaffCode = null,
          arrivals = mutableListOf(),
          departures = mutableListOf(),
          nonArrival = null,
          cancellations = mutableListOf(),
          confirmation = null,
          extensions = mutableListOf(),
          dateChanges = mutableListOf(),
          premises = premises!!,
          bed = bed,
          service = ServiceName.approvedPremises.value,
          originalArrivalDate = arrivalDate,
          originalDepartureDate = departureDate,
          createdAt = bookingCreatedAt,
          application = placementRequest.application,
          offlineApplication = null,
          turnarounds = mutableListOf(),
          nomsNumber = placementRequest.application.nomsNumber,
          placementRequest = null,
          status = BookingStatus.confirmed,
          adhoc = false,
        ),
      )

      placementRequest.booking = booking

      cas1ApplicationStatusService.bookingMade(booking)
      placementRequestRepository.save(placementRequest)

      val application = placementRequest.application

      cas1BookingDomainEventService.bookingMade(
        application = application,
        booking = booking,
        user = user,
        placementRequest = placementRequest,
      )
      cas1BookingEmailService.bookingMade(
        application = placementRequest.application,
        booking = booking,
        placementApplication = placementRequest.placementApplication,
      )

      return@validated success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  @SuppressWarnings("UnusedParameter")
  @Transactional
  fun createArrival(
    user: UserEntity,
    booking: BookingEntity,
    arrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    notes: String?,
    keyWorkerStaffCode: String?,
  ) = validated<ArrivalEntity> {
    if (booking.premises is TemporaryAccommodationPremisesEntity) {
      return generalError("CAS3 booking arrival not supported here, preferred method is createArrival in Cas3BookingService")
    }

    if (booking.arrival != null) {
      return generalError("This Booking already has an Arrival set")
    }

    if (expectedDepartureDate.isBefore(arrivalDate)) {
      return "$.expectedDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }

    val arrivalEntity = arrivalRepository.save(
      ArrivalEntity(
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
    booking.status = BookingStatus.arrived
    updateBooking(booking)

    booking.arrivals += arrivalEntity

    return success(arrivalEntity)
  }

  fun getWithdrawableState(booking: BookingEntity, user: UserEntity): WithdrawableState {
    return WithdrawableState(
      withdrawable = booking.isInCancellableStateCas1(),
      withdrawn = booking.isCancelled,
      userMayDirectlyWithdraw = userAccessService.userMayCancelBooking(user, booking),
      blockingReason = if (booking.hasArrivals()) {
        BlockingReason.ArrivalRecordedInCas1
      } else if (deliusService.referralHasArrival(booking)) {
        BlockingReason.ArrivalRecordedInDelius
      } else {
        null
      },
    )
  }

  /**
   * In CAS1 there are some legacy applications where the adhoc status is not known, indicated
   * by the adhoc column being null. These are typically treated the same as adhoc
   * bookings for certain operations (e.g. withdrawals)
   */
  fun getAllAdhocOrUnknownForApplication(applicationEntity: ApplicationEntity) =
    bookingRepository.findAllAdhocOrUnknownByApplication(applicationEntity)

  /**
   * This function should not be called directly. Instead, use [WithdrawableService.withdrawBooking] that
   * will indirectly invoke this function. It will also ensure that:
   *
   * 1. The entity is withdrawable, and error if not
   * 2. The user is allowed to withdraw it, and error if not
   * 3. If withdrawn, all descendants entities are withdrawn, where applicable
   */
  @SuppressWarnings("ReturnCount")
  @Transactional
  fun createCas1Cancellation(
    booking: BookingEntity,
    cancelledAt: LocalDate,
    userProvidedReason: UUID?,
    notes: String?,
    otherReason: String?,
    withdrawalContext: WithdrawalContext,
  ): CasResult<CancellationEntity> {
    if (booking.application != null && booking.application !is ApprovedPremisesApplicationEntity) {
      return CasResult.GeneralValidationError("Application is not for CAS1")
    }

    val existingCancellation = booking.cancellation
    if (booking.premises is ApprovedPremisesEntity && existingCancellation != null) {
      return CasResult.Success(existingCancellation)
    }

    val resolvedReasonId = toCas1CancellationReason(withdrawalContext, userProvidedReason)

    val reason = cancellationReasonRepository.findByIdOrNull(resolvedReasonId)
    if (reason == null) {
      return CasResult.FieldValidationError(mapOf("$.reason" to "doesNotExist"))
    } else if (!reason.serviceScopeMatches(booking.service)) {
      return CasResult.FieldValidationError(mapOf("$.reason" to "incorrectCancellationReasonServiceScope"))
    }

    if (reason.name == "Other" && otherReason.isNullOrEmpty()) {
      return CasResult.FieldValidationError(mapOf("$.otherReason" to "empty"))
    }

    val cancellationEntity = cancellationRepository.save(
      CancellationEntity(
        id = UUID.randomUUID(),
        date = cancelledAt,
        reason = reason,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
        otherReason = otherReason,
      ),
    )
    booking.status = BookingStatus.cancelled
    updateBooking(booking)
    booking.cancellations += cancellationEntity

    val user = when (withdrawalContext.withdrawalTriggeredBy) {
      is WithdrawalTriggeredBySeedJob -> null
      is WithdrawalTriggeredByUser -> withdrawalContext.withdrawalTriggeredBy.user
    }
    if (shouldCreateDomainEventForBooking(booking, user)) {
      cas1BookingDomainEventService.bookingCancelled(booking, user!!, cancellationEntity, reason)
    }

    cas1ApplicationStatusService.lastBookingCancelled(
      booking = booking,
      isUserRequestedWithdrawal = withdrawalContext.triggeringEntityType == WithdrawableEntityType.Booking,
    )

    val application = booking.application as ApprovedPremisesApplicationEntity?
    application?.let {
      cas1BookingEmailService.bookingWithdrawn(
        application = it,
        booking = booking,
        placementApplication = booking.placementRequest?.placementApplication,
        withdrawalTriggeredBy = withdrawalContext.withdrawalTriggeredBy,
      )
    }

    return CasResult.Success(cancellationEntity)
  }

  private fun toCas1CancellationReason(
    withdrawalContext: WithdrawalContext,
    userProvidedReason: UUID?,
  ) = when (withdrawalContext.triggeringEntityType) {
    WithdrawableEntityType.Application -> CAS1_RELATED_APP_WITHDRAWN_ID
    WithdrawableEntityType.PlacementApplication -> CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
    WithdrawableEntityType.PlacementRequest -> CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
    WithdrawableEntityType.Booking -> userProvidedReason
    WithdrawableEntityType.SpaceBooking -> throw InternalServerErrorProblem("Withdrawing a SpaceBooking should not cascade to Booking")
  }

  @Transactional
  fun createConfirmation(
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    notes: String?,
  ) = validated<ConfirmationEntity> {
    if (booking.confirmation != null) {
      return generalError("This Booking already has a Confirmation set")
    }

    val confirmationEntity = confirmationRepository.save(
      ConfirmationEntity(
        id = UUID.randomUUID(),
        dateTime = dateTime,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )
    booking.status = BookingStatus.confirmed
    updateBooking(booking)
    booking.confirmation = confirmationEntity

    return success(confirmationEntity)
  }

  private fun shouldCreateDomainEventForBooking(booking: BookingEntity, user: UserEntity?) =
    booking.service == ServiceName.approvedPremises.value && user != null && (booking.application != null || booking.offlineApplication?.eventNumber != null)

  @SuppressWarnings("CyclomaticComplexMethod")
  @Transactional
  fun createDateChange(
    booking: BookingEntity,
    user: UserEntity,
    newArrivalDate: LocalDate?,
    newDepartureDate: LocalDate?,
  ) = validatedCasResult {
    val effectiveNewArrivalDate = newArrivalDate ?: booking.arrivalDate
    val effectiveNewDepartureDate = newDepartureDate ?: booking.departureDate

    val expectedLastUnavailableDate =
      workingDayService.addWorkingDays(effectiveNewDepartureDate, booking.turnaround?.workingDayCount ?: 0)

    if (booking.isCancelled) {
      return generalError("This Booking is cancelled and as such cannot be modified")
    }

    if (booking.service != ServiceName.approvedPremises.value) {
      val bedId = booking.bed?.id
        ?: throw InternalServerErrorProblem("No bed ID present on Booking: ${booking.id}")

      getBookingWithConflictingDates(effectiveNewArrivalDate, expectedLastUnavailableDate, booking.id, bedId)?.let {
        return@validatedCasResult it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
      }

      getVoidBedspaceWithConflictingDates(effectiveNewArrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validatedCasResult it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }
    }

    if (effectiveNewArrivalDate.isAfter(effectiveNewDepartureDate)) {
      return "$.newDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }

    if (booking.arrival != null) {
      if (effectiveNewArrivalDate != booking.arrivalDate) {
        return "$.newArrivalDate" hasSingleValidationError "arrivalDateCannotBeChangedOnArrivedBooking"
      }
    }

    val previousArrivalDate = booking.arrivalDate
    val previousDepartureDate = booking.departureDate

    val dateChangeEntity = dateChangeRepository.save(
      DateChangeEntity(
        id = UUID.randomUUID(),
        previousArrivalDate = previousArrivalDate,
        previousDepartureDate = previousDepartureDate,
        newArrivalDate = effectiveNewArrivalDate,
        newDepartureDate = effectiveNewDepartureDate,
        changedAt = OffsetDateTime.now(),
        booking = booking,
        changedByUser = user,
      ),
    )

    updateBooking(
      booking.apply {
        arrivalDate = effectiveNewArrivalDate
        departureDate = effectiveNewDepartureDate
        dateChanges.add(dateChangeEntity)
      },
    )

    if (shouldCreateDomainEventForBooking(booking, user)) {
      cas1BookingDomainEventService.bookingChanged(
        booking = booking,
        changedBy = user,
        bookingChangedAt = OffsetDateTime.now(),
        previousArrivalDateIfChanged = if (previousArrivalDate != effectiveNewArrivalDate) {
          previousArrivalDate
        } else {
          null
        },
        previousDepartureDateIfChanged = if (previousDepartureDate != effectiveNewDepartureDate) {
          previousDepartureDate
        } else {
          null
        },
      )
    }

    return success(dateChangeEntity)
  }

  fun getBookingForPremises(premisesId: UUID, bookingId: UUID): GetBookingForPremisesResult {
    val premises = premisesService.getPremises(premisesId)
      ?: return GetBookingForPremisesResult.PremisesNotFound

    val booking = bookingRepository.findByIdOrNull(bookingId)
      ?: return GetBookingForPremisesResult.BookingNotFound

    if (booking.premises.id != premises.id) {
      return GetBookingForPremisesResult.BookingNotFound
    }

    return GetBookingForPremisesResult.Success(booking)
  }

  fun getBookingWithConflictingDates(
    arrivalDate: LocalDate,
    closedDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ): BookingEntity? {
    val candidateBookings = bookingRepository.findByBedIdAndArrivingBeforeDate(bedId, closedDate, thisEntityId)

    return candidateBookings.firstOrNull { it.lastUnavailableDate >= arrivalDate }
  }

  fun getVoidBedspaceWithConflictingDates(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) = cas3VoidBedspacesRepository.findByBedIdAndOverlappingDate(
    bedId,
    startDate,
    endDate,
    thisEntityId,
  ).firstOrNull()

  val BookingEntity.lastUnavailableDate: LocalDate
    get() = workingDayService.addWorkingDays(this.departureDate, this.turnaround?.workingDayCount ?: 0)
}

sealed interface GetBookingForPremisesResult {
  data class Success(val booking: BookingEntity) : GetBookingForPremisesResult
  object PremisesNotFound : GetBookingForPremisesResult
  object BookingNotFound : GetBookingForPremisesResult
}
