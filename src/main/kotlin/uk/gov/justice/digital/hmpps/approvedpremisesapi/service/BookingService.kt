package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDeparted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedDestination
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.util.UUID
import javax.transaction.Transactional

@Service
class BookingService(
  private val premisesService: PremisesService,
  private val staffMemberService: StaffMemberService,
  private val offenderService: OffenderService,
  private val domainEventService: DomainEventService,
  private val cruService: CruService,
  private val applicationService: ApplicationService,
  private val workingDayCountService: WorkingDayCountService,
  private val emailNotificationService: EmailNotificationService,
  private val placementRequestService: PlacementRequestService,
  private val communityApiClient: CommunityApiClient,
  private val bookingRepository: BookingRepository,
  private val arrivalRepository: ArrivalRepository,
  private val cancellationRepository: CancellationRepository,
  private val confirmationRepository: ConfirmationRepository,
  private val extensionRepository: ExtensionRepository,
  private val dateChangeRepository: DateChangeRepository,
  private val departureRepository: DepartureRepository,
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val destinationProviderRepository: DestinationProviderRepository,
  private val nonArrivalRepository: NonArrivalRepository,
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val bedRepository: BedRepository,
  private val placementRequestRepository: PlacementRequestRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val turnaroundRepository: TurnaroundRepository,
  private val bedMoveRepository: BedMoveRepository,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.booking}") private val bookingUrlTemplate: String,
) {
  val approvedPremisesBookingAppealedCancellationReasonId: UUID = UUID.fromString("acba3547-ab22-442d-acec-2652e49895f2")

  fun updateBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)
  fun getBooking(id: UUID) = bookingRepository.findByIdOrNull(id)

  @Transactional
  fun createApprovedPremisesBookingFromPlacementRequest(
    user: UserEntity,
    placementRequestId: UUID,
    bedId: UUID,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return AuthorisableActionResult.NotFound("PlacementRequest", placementRequestId.toString())

    if (placementRequest.allocatedToUser.id != user.id) {
      return AuthorisableActionResult.Unauthorised()
    }

    val validationResult = validated {
      if (placementRequest.booking != null) {
        return@validated placementRequest.booking!!.id hasConflictError "A Booking has already been made for this Placement Request"
      }

      if (departureDate.isBefore(arrivalDate)) {
        "$.departureDate" hasValidationError "beforeBookingArrivalDate"
      }

      getBookingWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates"
      }

      getLostBedWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }

      val bed = bedRepository.findByIdOrNull(bedId)

      if (bed == null) {
        "$.bedId" hasValidationError "doesNotExist"
      } else if (bed.room.premises !is ApprovedPremisesEntity) {
        "$.bedId" hasValidationError "mustBelongToApprovedPremises"
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
          arrival = null,
          departures = mutableListOf(),
          nonArrival = null,
          cancellations = mutableListOf(),
          confirmation = null,
          extensions = mutableListOf(),
          dateChanges = mutableListOf(),
          premises = bed!!.room.premises,
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
        ),
      )

      placementRequest.booking = booking
      placementRequestRepository.save(placementRequest)

      saveBookingMadeDomainEvent(
        booking = booking,
        user = user,
        bookingCreatedAt = bookingCreatedAt,
      )

      val applicationSubmittedByUser = placementRequest.application.createdByUser

      val lengthOfStayDays = arrivalDate.getDaysUntilInclusive(departureDate).size
      val lengthOfStayWeeks = lengthOfStayDays.toDouble() / 7
      val lengthOfStayWeeksWholeNumber = (lengthOfStayDays.toDouble() % 7) == 0.0

      emailNotificationService.sendEmail(
        user = applicationSubmittedByUser,
        templateId = notifyConfig.templates.bookingMade,
        personalisation = mapOf(
          "name" to applicationSubmittedByUser.name,
          "apName" to bed.room.premises.name,
          "applicationUrl" to applicationUrlTemplate.replace("#id", placementRequest.application.id.toString()),
          "bookingUrl" to bookingUrlTemplate.replace("#premisesId", booking.premises.id.toString())
            .replace("#bookingId", booking.id.toString()),
          "crn" to placementRequest.application.crn,
          "startDate" to arrivalDate.toString(),
          "endDate" to departureDate.toString(),
          "lengthStay" to if (lengthOfStayWeeksWholeNumber) lengthOfStayWeeks else lengthOfStayDays,
          "lengthStayUnit" to if (lengthOfStayWeeksWholeNumber) "weeks" else "days",
        ),
      )

      return@validated success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  @Transactional
  fun moveBooking(
    booking: BookingEntity,
    bedId: UUID,
    notes: String?,
    user: UserEntity,
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    if (user != null && (!user.hasRole(UserRole.CAS1_MANAGER))) {
      return AuthorisableActionResult.Unauthorised()
    }

    val newBed = bedRepository.findByIdOrNull(bedId) ?: return AuthorisableActionResult.NotFound("Bed", bedId.toString())

    val validationResult = validated {
      if (newBed.room.premises !is ApprovedPremisesEntity) {
        "$.bedId" hasValidationError "mustBelongToApprovedPremises"
      } else if (newBed.room.premises != booking.bed!!.room.premises) {
        "$.bedId" hasValidationError "mustBelongToTheSamePremises"
      }

      if (validationErrors.any()) {
        return@validated fieldValidationError
      }

      val bedMove = BedMoveEntity(
        id = UUID.randomUUID(),
        booking = booking,
        previousBed = booking.bed!!,
        newBed = newBed!!,
        createdAt = OffsetDateTime.now(),
        notes = notes,
      )

      booking.bed = newBed

      bookingRepository.save(booking)
      bedMoveRepository.save(bedMove)

      success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  @Transactional
  fun createApprovedPremisesAdHocBooking(
    user: UserEntity? = null,
    crn: String,
    nomsNumber: String?,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    bedId: UUID,
    bookingId: UUID? = null,
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    val bookingId = bookingId ?: UUID.randomUUID()

    if (user != null && (!user.hasAnyRole(UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER))) {
      return AuthorisableActionResult.Unauthorised()
    }

    val validationResult = validated {
      if (departureDate.isBefore(arrivalDate)) {
        "$.departureDate" hasValidationError "beforeBookingArrivalDate"
      }

      getLostBedWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }

      val bed = bedRepository.findByIdOrNull(bedId)

      if (bed == null) {
        "$.bedId" hasValidationError "doesNotExist"
      } else if (bed.room.premises !is ApprovedPremisesEntity) {
        "$.bedId" hasValidationError "mustBelongToApprovedPremises"
      }

      val newestSubmittedOnlineApplication = applicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises)
        .filter { it.submittedAt != null }
        .maxByOrNull { it.submittedAt!! }
      var newestOfflineApplication = applicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises)
        .maxByOrNull { it.createdAt }

      if (newestSubmittedOnlineApplication == null && newestOfflineApplication == null) {
        newestOfflineApplication = applicationService.createOfflineApplication(
          OfflineApplicationEntity(
            id = UUID.randomUUID(),
            crn = crn,
            service = ServiceName.approvedPremises.value,
            createdAt = OffsetDateTime.now(),
          ),
        )
      }

      if (validationErrors.any()) {
        return@validated fieldValidationError
      }

      val associateWithOfflineApplication = (newestOfflineApplication != null && newestSubmittedOnlineApplication == null) ||
        (newestOfflineApplication != null && newestSubmittedOnlineApplication != null && newestOfflineApplication.createdAt > newestSubmittedOnlineApplication.submittedAt)

      val associateWithOnlineApplication = newestSubmittedOnlineApplication != null && !associateWithOfflineApplication

      val bookingCreatedAt = OffsetDateTime.now()

      val booking = bookingRepository.save(
        BookingEntity(
          id = bookingId,
          crn = crn,
          arrivalDate = arrivalDate,
          departureDate = departureDate,
          keyWorkerStaffCode = null,
          arrival = null,
          departures = mutableListOf(),
          nonArrival = null,
          cancellations = mutableListOf(),
          confirmation = null,
          extensions = mutableListOf(),
          premises = bed!!.room.premises,
          bed = bed,
          service = ServiceName.approvedPremises.value,
          originalArrivalDate = arrivalDate,
          originalDepartureDate = departureDate,
          createdAt = bookingCreatedAt,
          application = if (associateWithOnlineApplication) newestSubmittedOnlineApplication else null,
          offlineApplication = if (associateWithOfflineApplication) newestOfflineApplication else null,
          turnarounds = mutableListOf(),
          dateChanges = mutableListOf(),
          nomsNumber = nomsNumber,
          placementRequest = null,
        ),
      )

      if (associateWithOnlineApplication && user != null) {
        saveBookingMadeDomainEvent(
          booking = booking,
          user = user,
          bookingCreatedAt = bookingCreatedAt,
        )

        val applicationSubmittedByUser = newestSubmittedOnlineApplication!!.createdByUser

        val lengthOfStayDays = arrivalDate.getDaysUntilInclusive(departureDate).size
        val lengthOfStayWeeks = lengthOfStayDays.toDouble() / 7
        val lengthOfStayWeeksWholeNumber = (lengthOfStayDays.toDouble() % 7) == 0.0

        emailNotificationService.sendEmail(
          user = applicationSubmittedByUser,
          templateId = notifyConfig.templates.bookingMade,
          personalisation = mapOf(
            "name" to applicationSubmittedByUser.name,
            "apName" to bed.room.premises.name,
            "applicationUrl" to applicationUrlTemplate.replace("#id", newestSubmittedOnlineApplication.id.toString()),
            "bookingUrl" to bookingUrlTemplate.replace("#premisesId", booking.premises.id.toString())
              .replace("#bookingId", booking.id.toString()),
            "crn" to crn,
            "startDate" to arrivalDate.toString(),
            "endDate" to departureDate.toString(),
            "lengthStay" to if (lengthOfStayWeeksWholeNumber) lengthOfStayWeeks.toInt() else lengthOfStayDays,
            "lengthStayUnit" to if (lengthOfStayWeeksWholeNumber) "weeks" else "days",
          ),
        )
      }

      success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  private fun saveBookingMadeDomainEvent(
    booking: BookingEntity,
    user: UserEntity,
    bookingCreatedAt: OffsetDateTime,
  ) {
    val domainEventId = UUID.randomUUID()

    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername, true)) {
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      else -> null
    }

    val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
    val staffDetails = when (staffDetailsResult) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    val application = booking.application!! as ApprovedPremisesApplicationEntity
    val approvedPremises = booking.premises as ApprovedPremisesEntity

    domainEventService.saveBookingMadeDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = booking.crn,
        occurredAt = bookingCreatedAt.toInstant(),
        data = BookingMadeEnvelope(
          id = domainEventId,
          timestamp = bookingCreatedAt.toInstant(),
          eventType = "approved-premises.booking.made",
          eventDetails = BookingMade(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
            bookingId = booking.id,
            personReference = PersonReference(
              crn = booking.application?.crn ?: booking.offlineApplication!!.crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            createdAt = bookingCreatedAt.toInstant(),
            bookedBy = BookingMadeBookedBy(
              staffMember = StaffMember(
                staffCode = staffDetails.staffCode,
                staffIdentifier = staffDetails.staffIdentifier,
                forenames = staffDetails.staff.forenames,
                surname = staffDetails.staff.surname,
                username = staffDetails.username,
              ),
              cru = Cru(
                name = cruService.cruNameFromProbationAreaCode(staffDetails.probationArea.code),
              ),
            ),
            premises = Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ),
            arrivalOn = booking.arrivalDate,
            departureOn = booking.departureDate,
          ),
        ),
      ),
    )
  }

  @Transactional
  fun createTemporaryAccommodationBooking(
    user: UserEntity,
    premises: TemporaryAccommodationPremisesEntity,
    crn: String,
    nomsNumber: String?,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    bedId: UUID,
    enableTurnarounds: Boolean,
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    val validationResult = validated {
      val expectedLastUnavailableDate = workingDayCountService.addWorkingDays(departureDate, premises.turnaroundWorkingDayCount)
      getBookingWithConflictingDates(arrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
      }

      getLostBedWithConflictingDates(arrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }

      val bed = bedRepository.findByIdOrNull(bedId)

      if (bed == null) {
        "$.bedId" hasValidationError "doesNotExist"
      } else if (bed.room.premises !is TemporaryAccommodationPremisesEntity) {
        "$.bedId" hasValidationError "mustBelongToTemporaryAccommodationPremises"
      }

      if (departureDate.isBefore(arrivalDate)) {
        "$.departureDate" hasValidationError "beforeBookingArrivalDate"
      }

      if (validationErrors.any()) {
        return@validated fieldValidationError
      }

      val bookingCreatedAt = OffsetDateTime.now()

      val booking = bookingRepository.save(
        BookingEntity(
          id = UUID.randomUUID(),
          crn = crn,
          nomsNumber = nomsNumber,
          arrivalDate = arrivalDate,
          departureDate = departureDate,
          keyWorkerStaffCode = null,
          arrival = null,
          departures = mutableListOf(),
          nonArrival = null,
          cancellations = mutableListOf(),
          confirmation = null,
          extensions = mutableListOf(),
          dateChanges = mutableListOf(),
          premises = premises,
          bed = bed,
          service = ServiceName.temporaryAccommodation.value,
          originalArrivalDate = arrivalDate,
          originalDepartureDate = departureDate,
          createdAt = bookingCreatedAt,
          application = null,
          offlineApplication = null,
          turnarounds = mutableListOf(),
          placementRequest = null,
        ),
      )

      val turnaround = turnaroundRepository.save(
        TurnaroundEntity(
          id = UUID.randomUUID(),
          workingDayCount = when (enableTurnarounds) {
            true -> premises.turnaroundWorkingDayCount
            else -> 0
          },
          createdAt = bookingCreatedAt,
          booking = booking,
        ),
      )

      booking.turnarounds += turnaround

      success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  @Transactional
  fun createTurnaround(
    booking: BookingEntity,
    workingDays: Int,
  ) = validated {
    if (workingDays <= 0) {
      "$.workingDays" hasValidationError "isNotAPositiveInteger"
    }

    val expectedLastUnavailableDate = workingDayCountService.addWorkingDays(booking.departureDate, workingDays)
    getBookingWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, booking.id, booking.bed!!.id)?.let {
      return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
    }

    getLostBedWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, null, booking.bed!!.id)?.let {
      return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val turnaround = turnaroundRepository.save(
      TurnaroundEntity(
        id = UUID.randomUUID(),
        workingDayCount = workingDays,
        createdAt = OffsetDateTime.now(),
        booking = booking,
      ),
    )

    return success(turnaround)
  }

  @Transactional
  fun createCas1Arrival(
    user: UserEntity? = null,
    booking: BookingEntity,
    arrivalDateTime: Instant,
    expectedDepartureDate: LocalDate,
    notes: String?,
    keyWorkerStaffCode: String?,
  ) = validated<ArrivalEntity> {
    val premises = booking.premises

    if (premises !is ApprovedPremisesEntity) {
      return generalError("CAS1 Arrivals cannot be set on non-CAS1 premises")
    }

    val occurredAt = OffsetDateTime.now()
    val arrivalDate = LocalDate.ofInstant(arrivalDateTime, ZoneOffset.UTC)

    if (booking.arrival != null) {
      return generalError("This Booking already has an Arrival set")
    }

    if (expectedDepartureDate.isBefore(arrivalDate)) {
      return "$.expectedDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }

    if (keyWorkerStaffCode == null) {
      return "$.keyWorkerStaffCode" hasSingleValidationError "empty"
    }

    val staffMemberResponse = staffMemberService.getStaffMemberByCode(keyWorkerStaffCode, premises.qCode)

    if (staffMemberResponse !is AuthorisableActionResult.Success) {
      return "$.keyWorkerStaffId" hasSingleValidationError "notFound"
    }

    updateBooking(booking.apply { this.keyWorkerStaffCode = keyWorkerStaffCode })

    val arrivalEntity = arrivalRepository.save(
      ArrivalEntity(
        id = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalDateTime = arrivalDateTime,
        expectedDepartureDate = expectedDepartureDate,
        notes = notes,
        booking = booking,
        createdAt = occurredAt,
      ),
    )

    booking.arrivalDate = arrivalDate
    booking.departureDate = expectedDepartureDate
    updateBooking(booking)

    if (booking.application != null && user != null) {
      val domainEventId = UUID.randomUUID()

      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        else -> null
      }

      val keyWorkerStaffDetailsResult = communityApiClient.getStaffUserDetailsForStaffCode(keyWorkerStaffCode!!)
      val keyWorkerStaffDetails = when (keyWorkerStaffDetailsResult) {
        is ClientResult.Success -> keyWorkerStaffDetailsResult.body
        is ClientResult.Failure -> keyWorkerStaffDetailsResult.throwException()
      }

      val application = booking.application!! as ApprovedPremisesApplicationEntity
      val approvedPremises = booking.premises as ApprovedPremisesEntity

      domainEventService.savePersonArrivedEvent(
        DomainEvent(
          id = domainEventId,
          applicationId = application.id,
          crn = booking.crn,
          occurredAt = arrivalDateTime,
          data = PersonArrivedEnvelope(
            id = domainEventId,
            timestamp = occurredAt.toInstant(),
            eventType = "approved-premises.person.arrived",
            eventDetails = PersonArrived(
              applicationId = application.id,
              applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
              bookingId = booking.id,
              personReference = PersonReference(
                crn = booking.crn,
                noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
              ),
              deliusEventNumber = application.eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
              ),
              applicationSubmittedOn = application.submittedAt!!.toLocalDate(),
              keyWorker = StaffMember(
                staffCode = keyWorkerStaffCode,
                staffIdentifier = keyWorkerStaffDetails.staffIdentifier,
                forenames = keyWorkerStaffDetails.staff.forenames,
                surname = keyWorkerStaffDetails.staff.surname,
                username = null,
              ),
              arrivedAt = arrivalDateTime,
              expectedDepartureOn = expectedDepartureDate,
              notes = notes,
            ),
          ),
        ),
      )
    }

    return success(arrivalEntity)
  }

  @Transactional
  fun createArrival(
    user: UserEntity? = null,
    booking: BookingEntity,
    arrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    notes: String?,
    keyWorkerStaffCode: String?,
  ) = validated<ArrivalEntity> {
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
    updateBooking(booking)

    return success(arrivalEntity)
  }

  fun createNonArrival(
    user: UserEntity?,
    booking: BookingEntity,
    date: LocalDate,
    reasonId: UUID,
    notes: String?,
  ) = validated<NonArrivalEntity> {
    val occurredAt = OffsetDateTime.now()

    if (booking.nonArrival != null) {
      return generalError("This Booking already has a Non Arrival set")
    }

    if (booking.arrivalDate.isAfter(date)) {
      "$.date" hasValidationError "afterBookingArrivalDate"
    }

    val reason = nonArrivalReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      "$.reason" hasValidationError "doesNotExist"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val nonArrivalEntity = nonArrivalRepository.save(
      NonArrivalEntity(
        id = UUID.randomUUID(),
        date = date,
        notes = notes,
        reason = reason!!,
        booking = booking,
        createdAt = occurredAt,
      ),
    )

    if (booking.service == ServiceName.approvedPremises.value && booking.application != null && user != null) {
      val domainEventId = UUID.randomUUID()

      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Unauthorised")
        is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Not Found")
      }

      val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
      val staffDetails = when (staffDetailsResult) {
        is ClientResult.Success -> staffDetailsResult.body
        is ClientResult.Failure -> staffDetailsResult.throwException()
      }

      val application = booking.application!! as ApprovedPremisesApplicationEntity
      val approvedPremises = booking.premises as ApprovedPremisesEntity

      domainEventService.savePersonNotArrivedEvent(
        DomainEvent(
          id = domainEventId,
          applicationId = application.id,
          crn = booking.crn,
          occurredAt = date.toLocalDateTime().toInstant(),
          data = PersonNotArrivedEnvelope(
            id = domainEventId,
            timestamp = occurredAt.toInstant(),
            eventType = "approved-premises.person.not-arrived",
            eventDetails = PersonNotArrived(
              applicationId = application.id,
              applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
              bookingId = booking.id,
              personReference = PersonReference(
                crn = booking.crn,
                noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
              ),
              deliusEventNumber = application.eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
              ),
              expectedArrivalOn = booking.originalArrivalDate,
              recordedBy = StaffMember(
                staffCode = staffDetails.staffCode,
                staffIdentifier = staffDetails.staffIdentifier,
                forenames = staffDetails.staff.forenames,
                surname = staffDetails.staff.surname,
                username = staffDetails.username,
              ),
              notes = notes,
              reason = reason.name,
              legacyReasonCode = reason.legacyDeliusReasonCode!!,
            ),
          ),
        ),
      )
    }

    return success(nonArrivalEntity)
  }

  @Transactional
  fun createCancellation(
    user: UserEntity?,
    booking: BookingEntity,
    cancelledAt: LocalDate,
    reasonId: UUID,
    notes: String?,
  ) = validated<CancellationEntity> {
    if (booking.premises is ApprovedPremisesEntity && booking.cancellation != null) {
      return generalError("This Booking already has a Cancellation set")
    }

    val reason = cancellationReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      "$.reason" hasValidationError "doesNotExist"
    } else if (!serviceScopeMatches(reason.serviceScope, booking)) {
      "$.reason" hasValidationError "incorrectCancellationReasonServiceScope"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val cancellationEntity = cancellationRepository.save(
      CancellationEntity(
        id = UUID.randomUUID(),
        date = cancelledAt,
        reason = reason!!,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )

    if (reason.id == approvedPremisesBookingAppealedCancellationReasonId && booking.placementRequest != null) {
      val placementRequest = booking.placementRequest!!
      placementRequestService.createPlacementRequest(
        placementRequirements = placementRequest.placementRequirements,
        placementDates = PlacementDates(
          expectedArrival = placementRequest.expectedArrival,
          duration = placementRequest.duration,
        ),
        notes = placementRequest.notes,
      )
    }

    if (booking.service == ServiceName.approvedPremises.value && booking.application != null && user != null) {
      val dateTime = OffsetDateTime.now()

      val domainEventId = UUID.randomUUID()

      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Booking Cancelled Domain Event: Unauthorised")
        is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Booking Cancelled Domain Event: Not Found")
      }

      val keyWorkerStaffDetailsResult = communityApiClient.getStaffUserDetailsForStaffCode(booking.keyWorkerStaffCode!!)
      val keyWorkerStaffDetails = when (keyWorkerStaffDetailsResult) {
        is ClientResult.Success -> keyWorkerStaffDetailsResult.body
        is ClientResult.Failure -> keyWorkerStaffDetailsResult.throwException()
      }

      val application = booking.application!! as ApprovedPremisesApplicationEntity
      val approvedPremises = booking.premises as ApprovedPremisesEntity

      domainEventService.saveBookingCancelledEvent(
        DomainEvent(
          id = domainEventId,
          applicationId = application.id,
          crn = booking.crn,
          occurredAt = dateTime.toInstant(),
          data = BookingCancelledEnvelope(
            id = domainEventId,
            timestamp = dateTime.toInstant(),
            eventType = "approved-premises.booking.cancelled",
            eventDetails = BookingCancelled(
              applicationId = application.id,
              applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
              bookingId = booking.id,
              personReference = PersonReference(
                crn = booking.crn,
                noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
              ),
              deliusEventNumber = application.eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
              ),
              cancelledBy = StaffMember(
                staffCode = user.deliusStaffCode!!,
                staffIdentifier = user.deliusStaffIdentifier,
                forenames = user.name.split(" ").dropLast(1).joinToString(" "),
                surname = keyWorkerStaffDetails.staff.surname.split(" ").last(),
                username = null,
              ),
              cancelledAt = cancelledAt.atTime(OffsetTime.MIN).toInstant(),
              cancellationReason = reason.name,
            ),
          ),
        ),
      )
    }

    return success(cancellationEntity)
  }

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

    return success(confirmationEntity)
  }

  fun createDeparture(
    user: UserEntity?,
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    reasonId: UUID,
    moveOnCategoryId: UUID,
    destinationProviderId: UUID?,
    notes: String?,
  ) = validated<DepartureEntity> {
    val occurredAt = OffsetDateTime.now()

    if (booking.premises is ApprovedPremisesEntity && booking.departure != null) {
      return generalError("This Booking already has a Departure set")
    }

    if (booking.arrivalDate.toLocalDateTime().isAfter(dateTime)) {
      "$.dateTime" hasValidationError "beforeBookingArrivalDate"
    }

    val reason = departureReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      "$.reasonId" hasValidationError "doesNotExist"
    } else if (!serviceScopeMatches(reason.serviceScope, booking)) {
      "$.reasonId" hasValidationError "incorrectDepartureReasonServiceScope"
    }

    val moveOnCategory = moveOnCategoryRepository.findByIdOrNull(moveOnCategoryId)
    if (moveOnCategory == null) {
      "$.moveOnCategoryId" hasValidationError "doesNotExist"
    } else if (!serviceScopeMatches(moveOnCategory.serviceScope, booking)) {
      "$.moveOnCategoryId" hasValidationError "incorrectMoveOnCategoryServiceScope"
    }

    val destinationProvider = when (booking.service) {
      ServiceName.approvedPremises.value -> {
        when (destinationProviderId) {
          null -> {
            "$.destinationProviderId" hasValidationError "empty"
            null
          }
          else -> {
            val result = destinationProviderRepository.findByIdOrNull(destinationProviderId)
            if (result == null) {
              "$.destinationProviderId" hasValidationError "doesNotExist"
            }
            result
          }
        }
      }
      else -> null
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val departureEntity = departureRepository.save(
      DepartureEntity(
        id = UUID.randomUUID(),
        dateTime = dateTime,
        reason = reason!!,
        moveOnCategory = moveOnCategory!!,
        destinationProvider = destinationProvider,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )

    booking.departureDate = dateTime.toLocalDate()
    updateBooking(booking)

    if (booking.service == ServiceName.approvedPremises.value && booking.application != null && user != null) {
      val domainEventId = UUID.randomUUID()

      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Unauthorised")
        is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Not Found")
      }

      val keyWorkerStaffDetailsResult = communityApiClient.getStaffUserDetailsForStaffCode(booking.keyWorkerStaffCode!!)
      val keyWorkerStaffDetails = when (keyWorkerStaffDetailsResult) {
        is ClientResult.Success -> keyWorkerStaffDetailsResult.body
        is ClientResult.Failure -> keyWorkerStaffDetailsResult.throwException()
      }

      val application = booking.application!! as ApprovedPremisesApplicationEntity
      val approvedPremises = booking.premises as ApprovedPremisesEntity

      domainEventService.savePersonDepartedEvent(
        DomainEvent(
          id = domainEventId,
          applicationId = application.id,
          crn = booking.crn,
          occurredAt = dateTime.toInstant(),
          data = PersonDepartedEnvelope(
            id = domainEventId,
            timestamp = occurredAt.toInstant(),
            eventType = "approved-premises.person.departed",
            eventDetails = PersonDeparted(
              applicationId = application.id,
              applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
              bookingId = booking.id,
              personReference = PersonReference(
                crn = booking.crn,
                noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
              ),
              deliusEventNumber = application.eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
              ),
              keyWorker = StaffMember(
                staffCode = user.deliusStaffCode!!,
                staffIdentifier = user.deliusStaffIdentifier,
                forenames = user.name.split(" ").dropLast(1).joinToString(" "),
                surname = keyWorkerStaffDetails.staff.surname.split(" ").last(),
                username = null,
              ),
              departedAt = dateTime.toInstant(),
              reason = reason.name,
              legacyReasonCode = reason.legacyDeliusReasonCode!!,
              destination = PersonDepartedDestination(
                moveOnCategory = MoveOnCategory(
                  description = moveOnCategory.name,
                  legacyMoveOnCategoryCode = moveOnCategory.legacyDeliusCategoryCode!!,
                  id = moveOnCategory.id,
                ),
                destinationProvider = DestinationProvider(
                  description = destinationProvider!!.name,
                  id = destinationProvider.id,
                ),
                premises = null,
              ),
            ),
          ),
        ),
      )
    }

    return success(departureEntity)
  }

  @Transactional
  fun createExtension(
    booking: BookingEntity,
    newDepartureDate: LocalDate,
    notes: String?,
  ) = validated<ExtensionEntity> {
    val expectedLastUnavailableDate = workingDayCountService.addWorkingDays(newDepartureDate, booking.turnaround?.workingDayCount ?: 0)

    val bedId = booking.bed?.id
      ?: throw InternalServerErrorProblem("No bed ID present on Booking: ${booking.id}")

    if (booking.service != ServiceName.approvedPremises.value) {
      getBookingWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, booking.id, bedId)?.let {
        return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
      }

      getLostBedWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }
    }

    if (booking.premises is ApprovedPremisesEntity && booking.departureDate.isAfter(newDepartureDate)) {
      return "$.newDepartureDate" hasSingleValidationError "beforeExistingDepartureDate"
    }

    if (booking.arrivalDate.isAfter(newDepartureDate)) {
      return "$.newDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }

    val extensionEntity = ExtensionEntity(
      id = UUID.randomUUID(),
      previousDepartureDate = booking.departureDate,
      newDepartureDate = newDepartureDate,
      notes = notes,
      booking = booking,
      createdAt = OffsetDateTime.now(),
    )

    val extension = extensionRepository.save(extensionEntity)
    booking.departureDate = extensionEntity.newDepartureDate
    booking.extensions.add(extension)
    updateBooking(booking)

    return success(extensionEntity)
  }

  @Transactional
  fun createDateChange(
    booking: BookingEntity,
    user: UserEntity,
    newArrivalDate: LocalDate?,
    newDepartureDate: LocalDate?,
  ) = validated {
    val effectiveNewArrivalDate = newArrivalDate ?: booking.arrivalDate
    val effectiveNewDepartureDate = newDepartureDate ?: booking.departureDate

    val expectedLastUnavailableDate = workingDayCountService.addWorkingDays(effectiveNewDepartureDate, booking.turnaround?.workingDayCount ?: 0)

    val bedId = booking.bed?.id
      ?: throw InternalServerErrorProblem("No bed ID present on Booking: ${booking.id}")

    if (booking.service != ServiceName.approvedPremises.value) {
      getBookingWithConflictingDates(effectiveNewArrivalDate, expectedLastUnavailableDate, booking.id, bedId)?.let {
        return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
      }

      getLostBedWithConflictingDates(effectiveNewArrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }
    }

    if (effectiveNewArrivalDate.isAfter(effectiveNewDepartureDate)) {
      return "$.newDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }

    if (booking.arrival != null) {
      if (effectiveNewArrivalDate != booking.arrivalDate) {
        return "$.newArrivalDate" hasSingleValidationError "arrivalDateCannotBeChangedOnArrivedBooking"
      }

      if (effectiveNewDepartureDate.isAfter(booking.departureDate)) {
        return "$.newDepartureDate" hasSingleValidationError "departureDateCannotBeExtendedOnArrivedBooking"
      }
    }

    val dateChangeEntity = dateChangeRepository.save(
      DateChangeEntity(
        id = UUID.randomUUID(),
        previousDepartureDate = booking.departureDate,
        previousArrivalDate = booking.arrivalDate,
        newArrivalDate = effectiveNewArrivalDate,
        newDepartureDate = effectiveNewDepartureDate,
        changedAt = OffsetDateTime.now(),
        booking = booking,
        changedByUser = user,
      ),
    )

    updateBooking(
      booking.apply {
        arrivalDate = dateChangeEntity.newArrivalDate
        departureDate = dateChangeEntity.newDepartureDate
        dateChanges.add(dateChangeEntity)
      },
    )

    // TODO: Emit booking changed domain event once format is agreed

    return success(dateChangeEntity)
  }

  fun getBookingForPremises(premisesId: UUID, bookingId: UUID): GetBookingForPremisesResult {
    val premises = premisesService.getPremises(premisesId)
      ?: return GetBookingForPremisesResult.PremisesNotFound

    val booking = getBooking(bookingId)
      ?: return GetBookingForPremisesResult.BookingNotFound

    if (booking.premises.id != premises.id) {
      return GetBookingForPremisesResult.BookingNotFound
    }

    return GetBookingForPremisesResult.Success(booking)
  }

  @Transactional
  fun deleteBooking(booking: BookingEntity) {
    if (booking.arrival != null) {
      arrivalRepository.delete(booking.arrival!!)
    }

    if (booking.departure != null) {
      departureRepository.delete(booking.departure!!)
    }

    if (booking.nonArrival != null) {
      nonArrivalRepository.delete(booking.nonArrival!!)
    }

    if (booking.cancellation != null) {
      cancellationRepository.delete(booking.cancellation!!)
    }

    if (booking.confirmation != null) {
      confirmationRepository.delete(booking.confirmation!!)
    }

    booking.extensions.forEach {
      extensionRepository.delete(it)
    }

    booking.turnarounds.forEach {
      turnaroundRepository.delete(it)
    }

    bookingRepository.delete(booking)
  }

  private fun serviceScopeMatches(scope: String, booking: BookingEntity): Boolean {
    return when (scope) {
      "*" -> true
      booking.service -> true
      else -> return false
    }
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

  fun getLostBedWithConflictingDates(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) = lostBedsRepository.findByBedIdAndOverlappingDate(
    bedId,
    startDate,
    endDate,
    thisEntityId,
  ).firstOrNull()

  val BookingEntity.lastUnavailableDate: LocalDate
    get() = workingDayCountService.addWorkingDays(this.departureDate, this.turnaround?.workingDayCount ?: 0)
}

sealed interface GetBookingForPremisesResult {
  data class Success(val booking: BookingEntity) : GetBookingForPremisesResult
  object PremisesNotFound : GetBookingForPremisesResult
  object BookingNotFound : GetBookingForPremisesResult
}
