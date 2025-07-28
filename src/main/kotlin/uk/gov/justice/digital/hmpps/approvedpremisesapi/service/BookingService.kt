package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BookingService(
  private val offenderDetailService: OffenderDetailService,
  private val workingDayService: WorkingDayService,
  private val bookingRepository: BookingRepository,
  private val arrivalRepository: ArrivalRepository,
  private val cas3ConfirmationRepository: Cas3ConfirmationRepository,
  private val dateChangeRepository: DateChangeRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val cas1BookingEmailService: Cas1BookingEmailService,
  private val deliusService: DeliusService,
  private val cas1BookingDomainEventService: Cas1BookingDomainEventService,
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

    val personInfo = offenderDetailService.getPersonInfoResult(booking.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    return AuthorisableActionResult.Success(BookingAndPersons(booking, personInfo))
  }

  data class BookingAndPersons(
    val booking: BookingEntity,
    val personInfo: PersonInfoResult,
  )

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

  fun getWithdrawableState(booking: BookingEntity, user: UserEntity): WithdrawableState = WithdrawableState(
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

  /**
   * In CAS1 there are some legacy applications where the adhoc status is not known, indicated
   * by the adhoc column being null. These are typically treated the same as adhoc
   * bookings for certain operations (e.g. withdrawals)
   */
  fun getAllAdhocOrUnknownForApplication(applicationEntity: ApplicationEntity) = bookingRepository.findAllAdhocOrUnknownByApplication(applicationEntity)

  @Transactional
  fun createConfirmation(
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    notes: String?,
  ) = validated<Cas3ConfirmationEntity> {
    if (booking.confirmation != null) {
      return generalError("This Booking already has a Confirmation set")
    }

    val cas3ConfirmationEntity = cas3ConfirmationRepository.save(
      Cas3ConfirmationEntity(
        id = UUID.randomUUID(),
        dateTime = dateTime,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )
    booking.status = BookingStatus.confirmed
    updateBooking(booking)
    booking.confirmation = cas3ConfirmationEntity

    return success(cas3ConfirmationEntity)
  }

  private fun shouldCreateDomainEventForBooking(booking: BookingEntity, user: UserEntity?): Boolean = booking.service == ServiceName.approvedPremises.value &&
    user != null &&
    (booking.application != null || booking.offlineApplication?.eventNumber != null)

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

    if (booking.service == ServiceName.approvedPremises.value) {
      val application = booking.application as ApprovedPremisesApplicationEntity?
      application?.let {
        cas1BookingEmailService.bookingAmended(
          application = it,
          booking = booking,
          placementApplication = booking.placementRequest?.placementApplication,
        )
      }
    }

    return success(dateChangeEntity)
  }

  fun getBookingForPremises(premises: PremisesEntity, bookingId: UUID): GetBookingForPremisesResult {
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
  ) = cas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
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
  object BookingNotFound : GetBookingForPremisesResult
}
