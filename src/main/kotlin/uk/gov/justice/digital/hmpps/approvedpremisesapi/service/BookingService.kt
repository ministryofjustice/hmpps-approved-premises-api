package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationRepository
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
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
  private val communityApiClient: CommunityApiClient,
  private val bookingRepository: BookingRepository,
  private val arrivalRepository: ArrivalRepository,
  private val cancellationRepository: CancellationRepository,
  private val confirmationRepository: ConfirmationRepository,
  private val extensionRepository: ExtensionRepository,
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
  @Value("\${application-url-template}") private val applicationUrlTemplate: String
) {
  fun updateBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)
  fun getBooking(id: UUID) = bookingRepository.findByIdOrNull(id)

  @Transactional
  fun createApprovedPremisesBookingFromPlacementRequest(
    user: UserEntity,
    placementRequestId: UUID,
    bedId: UUID,
    arrivalDate: LocalDate,
    departureDate: LocalDate
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return AuthorisableActionResult.NotFound("PlacementRequest", placementRequestId.toString())

    if (placementRequest.allocatedToUser.id != user.id) {
      return AuthorisableActionResult.Unauthorised()
    }

    val validationResult = validated {
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
          premises = bed!!.room.premises,
          bed = bed,
          service = ServiceName.approvedPremises.value,
          originalArrivalDate = arrivalDate,
          originalDepartureDate = departureDate,
          createdAt = bookingCreatedAt,
          application = placementRequest.application,
          offlineApplication = null
        )
      )

      placementRequest.booking = booking
      placementRequestRepository.save(placementRequest)

      saveBookingMadeDomainEvent(
        booking = booking,
        user = user,
        bookingCreatedAt = bookingCreatedAt
      )

      return@validated success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  @Transactional
  fun createApprovedPremisesAdHocBooking(
    user: UserEntity,
    crn: String,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    bedId: UUID
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    if (!user.hasAnyRole(UserRole.MANAGER, UserRole.MATCHER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val validationResult = validated {
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

      val newestSubmittedOnlineApplication = applicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises)
        .filter { it.submittedAt != null }
        .maxByOrNull { it.submittedAt!! }
      val newestOfflineApplication = applicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises)
        .maxByOrNull { it.submittedAt }

      if (newestSubmittedOnlineApplication == null && newestOfflineApplication == null) {
        validationErrors["$.crn"] = "doesNotHaveApplication"
      }

      if (validationErrors.any()) {
        return@validated fieldValidationError
      }

      val associateWithOfflineApplication = (newestOfflineApplication != null && newestSubmittedOnlineApplication == null) ||
        (newestOfflineApplication != null && newestSubmittedOnlineApplication != null && newestOfflineApplication.submittedAt > newestSubmittedOnlineApplication.submittedAt)

      val associateWithOnlineApplication = newestSubmittedOnlineApplication != null && ! associateWithOfflineApplication

      val bookingCreatedAt = OffsetDateTime.now()

      val booking = bookingRepository.save(
        BookingEntity(
          id = UUID.randomUUID(),
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
          offlineApplication = if (associateWithOfflineApplication) newestOfflineApplication else null
        )
      )

      if (associateWithOnlineApplication) {
        saveBookingMadeDomainEvent(
          booking = booking,
          user = user,
          bookingCreatedAt = bookingCreatedAt
        )
      }

      success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  private fun saveBookingMadeDomainEvent(
    booking: BookingEntity,
    user: UserEntity,
    bookingCreatedAt: OffsetDateTime
  ) {
    val domainEventId = UUID.randomUUID()

    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername)) {
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
              noms = offenderDetails.otherIds.nomsNumber!!
            ),
            deliusEventNumber = application.eventNumber,
            createdAt = bookingCreatedAt.toInstant(),
            bookedBy = BookingMadeBookedBy(
              staffMember = StaffMember(
                staffCode = staffDetails.staffCode,
                staffIdentifier = staffDetails.staffIdentifier,
                forenames = staffDetails.staff.forenames,
                surname = staffDetails.staff.surname,
                username = staffDetails.username
              ),
              cru = Cru(
                name = cruService.cruNameFromProbationAreaCode(staffDetails.probationArea.code)
              )
            ),
            premises = Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name
            ),
            arrivalOn = booking.arrivalDate,
            departureOn = booking.departureDate
          )
        )
      )
    )
  }

  @Transactional
  fun createTemporaryAccommodationBooking(
    user: UserEntity,
    premises: TemporaryAccommodationPremisesEntity,
    crn: String,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    bedId: UUID
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    val validationResult = validated {
      getBookingWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates"
      }

      getLostBedWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
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
          arrivalDate = arrivalDate,
          departureDate = departureDate,
          keyWorkerStaffCode = null,
          arrival = null,
          departures = mutableListOf(),
          nonArrival = null,
          cancellations = mutableListOf(),
          confirmation = null,
          extensions = mutableListOf(),
          premises = premises,
          bed = bed,
          service = ServiceName.temporaryAccommodation.value,
          originalArrivalDate = arrivalDate,
          originalDepartureDate = departureDate,
          createdAt = bookingCreatedAt,
          application = null,
          offlineApplication = null
        )
      )

      success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  @Transactional
  fun createArrival(
    user: UserEntity,
    booking: BookingEntity,
    arrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    notes: String?,
    keyWorkerStaffCode: String?,
  ) = validated<ArrivalEntity> {
    val premises = booking.premises
    val occurredAt = OffsetDateTime.now()

    if (booking.arrival != null) {
      return generalError("This Booking already has an Arrival set")
    }

    if (expectedDepartureDate.isBefore(arrivalDate)) {
      return "$.expectedDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }

    if (premises is ApprovedPremisesEntity) {
      if (keyWorkerStaffCode == null) {
        return "$.keyWorkerStaffCode" hasSingleValidationError "empty"
      }

      val staffMemberResponse = staffMemberService.getStaffMemberByCode(keyWorkerStaffCode, premises.qCode)

      if (staffMemberResponse !is AuthorisableActionResult.Success) {
        return "$.keyWorkerStaffId" hasSingleValidationError "notFound"
      }

      updateBooking(booking.apply { this.keyWorkerStaffCode = keyWorkerStaffCode })
    }

    val arrivalEntity = arrivalRepository.save(
      ArrivalEntity(
        id = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        notes = notes,
        booking = booking,
        createdAt = occurredAt,
      )
    )

    booking.arrivalDate = arrivalDate
    booking.departureDate = expectedDepartureDate
    updateBooking(booking)

    if (booking.service == ServiceName.approvedPremises.value && booking.application != null) {
      val domainEventId = UUID.randomUUID()

      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Unauthorised")
        is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Not Found")
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
          occurredAt = arrivalDate.toLocalDateTime().toInstant(), // TODO: Endpoint should accept a date-time instead
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
                noms = offenderDetails.otherIds.nomsNumber!!
              ),
              deliusEventNumber = application.eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name
              ),
              applicationSubmittedOn = application.submittedAt!!.toLocalDate(),
              keyWorker = StaffMember(
                staffCode = keyWorkerStaffCode,
                staffIdentifier = keyWorkerStaffDetails.staffIdentifier,
                forenames = keyWorkerStaffDetails.staff.forenames,
                surname = keyWorkerStaffDetails.staff.surname,
                username = null
              ),
              arrivedAt = arrivalDate.toLocalDateTime().toInstant(), // TODO: Endpoint should accept a date-time instead
              expectedDepartureOn = expectedDepartureDate,
              notes = notes
            )
          )
        )
      )
    }

    return success(arrivalEntity)
  }

  fun createNonArrival(
    user: UserEntity,
    booking: BookingEntity,
    date: LocalDate,
    reasonId: UUID,
    notes: String?
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
      )
    )

    if (booking.service == ServiceName.approvedPremises.value && booking.application != null) {
      val domainEventId = UUID.randomUUID()

      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername)) {
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
                noms = offenderDetails.otherIds.nomsNumber!!
              ),
              deliusEventNumber = application.eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name
              ),
              expectedArrivalOn = booking.originalArrivalDate,
              recordedBy = StaffMember(
                staffCode = staffDetails.staffCode,
                staffIdentifier = staffDetails.staffIdentifier,
                forenames = staffDetails.staff.forenames,
                surname = staffDetails.staff.surname,
                username = staffDetails.username
              ),
              notes = notes
            )
          )
        )
      )
    }

    return success(nonArrivalEntity)
  }

  fun createCancellation(
    booking: BookingEntity,
    date: LocalDate,
    reasonId: UUID,
    notes: String?
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
        date = date,
        reason = reason!!,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      )
    )

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
      )
    )

    return success(confirmationEntity)
  }

  fun createDeparture(
    user: UserEntity,
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    reasonId: UUID,
    moveOnCategoryId: UUID,
    destinationProviderId: UUID?,
    notes: String?
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
      )
    )

    booking.departureDate = dateTime.toLocalDate()
    updateBooking(booking)

    if (booking.service == ServiceName.approvedPremises.value && booking.application != null) {
      val domainEventId = UUID.randomUUID()

      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername)) {
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
                noms = offenderDetails.otherIds.nomsNumber!!
              ),
              deliusEventNumber = application.eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name
              ),
              keyWorker = StaffMember(
                staffCode = booking.keyWorkerStaffCode!!,
                staffIdentifier = keyWorkerStaffDetails.staffIdentifier,
                forenames = keyWorkerStaffDetails.staff.forenames,
                surname = keyWorkerStaffDetails.staff.surname,
                username = null
              ),
              departedAt = dateTime.toInstant(),
              reason = reason.name,
              legacyReasonCode = reason.legacyDeliusReasonCode!!,
              destination = PersonDepartedDestination(
                moveOnCategory = MoveOnCategory(
                  description = moveOnCategory.name,
                  legacyMoveOnCategoryCode = moveOnCategory.legacyDeliusCategoryCode!!,
                  id = moveOnCategory.id
                ),
                destinationProvider = DestinationProvider(
                  description = destinationProvider!!.name,
                  id = destinationProvider.id
                ),
                premises = null
              )
            )
          )
        )
      )
    }

    return success(departureEntity)
  }

  @Transactional
  fun createExtension(
    booking: BookingEntity,
    newDepartureDate: LocalDate,
    notes: String?
  ) = validated<ExtensionEntity> {
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
    departureDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID
  ) = bookingRepository.findByBedIdAndOverlappingDate(
    bedId,
    arrivalDate,
    departureDate,
    thisEntityId
  )

  fun getLostBedWithConflictingDates(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID
  ) = lostBedsRepository.findByBedIdAndOverlappingDate(
    bedId,
    startDate,
    endDate,
    thisEntityId
  )
}

sealed interface GetBookingForPremisesResult {
  data class Success(val booking: BookingEntity) : GetBookingForPremisesResult
  object PremisesNotFound : GetBookingForPremisesResult
  object BookingNotFound : GetBookingForPremisesResult
}
