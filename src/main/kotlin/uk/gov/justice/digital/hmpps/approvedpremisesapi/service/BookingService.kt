package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import arrow.core.Either
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_APP_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.BookingListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredBySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromNestedAuthorisableValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.transaction.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService as Cas3DomainEventService

@Service
class BookingService(
  private val premisesService: PremisesService,
  private val staffMemberService: StaffMemberService,
  private val offenderService: OffenderService,
  private val domainEventService: DomainEventService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val applicationService: ApplicationService,
  private val workingDayService: WorkingDayService,
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
  private val premisesRepository: PremisesRepository,
  private val assessmentRepository: AssessmentRepository,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
  @Value("\${arrived-departed-domain-events-disabled}") private val arrivedAndDepartedDomainEventsDisabled: Boolean,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val assessmentService: AssessmentService,
  private val cas1BookingEmailService: Cas1BookingEmailService,
  private val deliusService: DeliusService,
  private val bookingListener: BookingListener,
  private val cas1BookingDomainEventService: Cas1BookingDomainEventService,
) {
  val approvedPremisesBookingAppealedCancellationReasonId: UUID =
    UUID.fromString("acba3547-ab22-442d-acec-2652e49895f2")

  private val log = LoggerFactory.getLogger(this::class.java)

  fun updateBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)

  fun getBooking(id: UUID): AuthorisableActionResult<BookingAndPersons> {
    val booking = bookingRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound("Booking", id.toString())

    val user = userService.getUserForRequest()

    if (!userAccessService.userCanViewBooking(user, booking)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val personInfo = offenderService.getPersonInfoResult(booking.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    val staffMember = booking.keyWorkerStaffCode?.let { keyWorkerStaffCode ->
      val premises = booking.premises

      // Bookings will need to be specialised in a similar way to Premises so that TA Bookings do not have a keyWorkerStaffCode field
      check(premises is ApprovedPremisesEntity) { "Booking has a Key Worker specified but Premises is not an ApprovedPremises" }

      when (val staffMemberResult = staffMemberService.getStaffMemberByCode(keyWorkerStaffCode, premises.qCode)) {
        is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
        is AuthorisableActionResult.NotFound -> {
          if (staffMemberResult.entityType == "Staff Code") {
            val error = "Unable to get Key Worker via Staff Code: $keyWorkerStaffCode"
            log.error(error)
            Sentry.captureException(InternalServerErrorProblem(error))
            null
          } else {
            throw InternalServerErrorProblem("Unable to get staff for QCode ${premises.qCode}")
          }
        }
        is AuthorisableActionResult.Success -> staffMemberResult.entity
      }
    }

    return AuthorisableActionResult.Success(BookingAndPersons(booking, personInfo, staffMember))
  }

  data class BookingAndPersons(
    val booking: BookingEntity,
    val personInfo: PersonInfoResult,
    val staffMember: uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember?,
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

    if (!user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER) && placementRequest.allocatedToUser?.id != user.id) {
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

      if (departureDate.isBefore(arrivalDate)) {
        "$.departureDate" hasValidationError "beforeBookingArrivalDate"
      }

      if (bedId != null) {
        getBookingWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
          return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates"
        }

        getLostBedWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
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
        return@validated generalError("You must specify either a bedId or a premisesId")
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

      bookingListener.prePersist(booking)
      placementRequestRepository.save(placementRequest)

      val application = placementRequest.application

      cas1BookingDomainEventService.bookingMade(
        application = application,
        booking = booking,
        user = user,
        placementRequest = placementRequest,
      )
      cas1BookingEmailService.bookingMade(placementRequest.application, booking)

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

    val newBed =
      bedRepository.findByIdOrNull(bedId) ?: return AuthorisableActionResult.NotFound("Bed", bedId.toString())

    val validationResult = validated {
      if (newBed.room.premises !is ApprovedPremisesEntity) {
        "$.bedId" hasValidationError "mustBelongToApprovedPremises"
      } else if (newBed.room.premises != booking.premises) {
        "$.bedId" hasValidationError "mustBelongToTheSamePremises"
      }

      if (validationErrors.any()) {
        return@validated fieldValidationError
      }

      val bedMove = BedMoveEntity(
        id = UUID.randomUUID(),
        booking = booking,
        previousBed = booking.bed,
        newBed = newBed,
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

  @Suppress("CyclomaticComplexMethod")
  @Transactional
  fun createApprovedPremisesAdHocBooking(
    user: UserEntity? = null,
    crn: String,
    nomsNumber: String?,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    premises: PremisesEntity,
    bedId: UUID?,
    eventNumber: String?,
    bookingId: UUID = UUID.randomUUID(),
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    if (user != null && (!user.hasAnyRole(UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER))) {
      return AuthorisableActionResult.Unauthorised()
    }

    val isCalledFromSeeder = user == null

    val validationResult = validated {
      if (departureDate.isBefore(arrivalDate)) {
        "$.departureDate" hasValidationError "beforeBookingArrivalDate"
      }

      val bed = bedId?.let {
        getLostBedWithConflictingDates(arrivalDate, departureDate, null, bedId)?.let {
          return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
        }

        val bed = bedRepository.findByIdOrNull(bedId)

        if (bed == null) {
          "$.bedId" hasValidationError "doesNotExist"
        } else if (bed.room.premises !is ApprovedPremisesEntity) {
          "$.bedId" hasValidationError "mustBelongToApprovedPremises"
        } else if (bed.room.premises != premises) {
          "$.bedId" hasValidationError "mustBelongToProvidedPremise"
        }

        bed
      }

      if (!isCalledFromSeeder && eventNumber == null) {
        "$.eventNumber" hasValidationError "empty"
      }

      if (validationErrors.any()) {
        return@validated fieldValidationError
      }

      val application = fetchApplication(crn, eventNumber)
      val onlineApplication =
        if (application is Either.Left<ApprovedPremisesApplicationEntity>) application.value else null
      val offlineApplication = if (application is Either.Right<OfflineApplicationEntity>) application.value else null

      val bookingCreatedAt = OffsetDateTime.now()

      val bookingPrePersist = BookingEntity(
        id = bookingId,
        crn = crn,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
        keyWorkerStaffCode = null,
        arrivals = mutableListOf(),
        departures = mutableListOf(),
        nonArrival = null,
        cancellations = mutableListOf(),
        confirmation = null,
        extensions = mutableListOf(),
        premises = premises,
        bed = bed,
        service = ServiceName.approvedPremises.value,
        originalArrivalDate = arrivalDate,
        originalDepartureDate = departureDate,
        createdAt = bookingCreatedAt,
        application = onlineApplication,
        offlineApplication = offlineApplication,
        turnarounds = mutableListOf(),
        dateChanges = mutableListOf(),
        nomsNumber = nomsNumber,
        placementRequest = null,
        status = BookingStatus.confirmed,
        adhoc = true,
      )

      bookingListener.prePersist(bookingPrePersist)
      val booking = bookingRepository.save(bookingPrePersist)

      if (!isCalledFromSeeder) {
        cas1BookingDomainEventService.adhocBookingMade(
          onlineApplication,
          offlineApplication,
          eventNumber,
          booking,
          user!!,
        )

        if (onlineApplication != null) {
          cas1BookingEmailService.bookingMade(onlineApplication, booking)
        }
      }

      success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  private fun fetchApplication(
    crn: String,
    eventNumber: String?,
  ): Either<ApprovedPremisesApplicationEntity, OfflineApplicationEntity> {
    val newestSubmittedOnlineApplication = applicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises)
      .filter { it.submittedAt != null }
      .maxByOrNull { it.submittedAt!! } as ApprovedPremisesApplicationEntity?
    var newestOfflineApplication = applicationService.getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises)
      .maxByOrNull { it.createdAt }

    if (newestSubmittedOnlineApplication == null && newestOfflineApplication == null) {
      log.info("No online or offline application, so we create a new offline application")
      newestOfflineApplication = applicationService.createOfflineApplication(
        OfflineApplicationEntity(
          id = UUID.randomUUID(),
          crn = crn,
          service = ServiceName.approvedPremises.value,
          createdAt = OffsetDateTime.now(),
          eventNumber = eventNumber,
        ),
      )
    }

    return if (newestOfflineApplication != null && newestSubmittedOnlineApplication != null && newestOfflineApplication.createdAt.isBefore(
        newestSubmittedOnlineApplication.submittedAt,
      )
    ) {
      log.info("Offline application is created before the online application, so returning the offline application with id ${newestOfflineApplication.id}")
      Either.Right(newestOfflineApplication)
    } else if (newestSubmittedOnlineApplication != null) {
      log.info("Returning online application with id ${newestSubmittedOnlineApplication.id}")
      Either.Left(newestSubmittedOnlineApplication)
    } else {
      log.info("Returning offline application with id ${newestOfflineApplication!!.id}")
      Either.Right(newestOfflineApplication)
    }
  }

  private fun saveBookingChangedDomainEvent(
    booking: BookingEntity,
    user: UserEntity,
    bookingChangedAt: OffsetDateTime,
  ) {
    val domainEventId = UUID.randomUUID()
    val (applicationId, eventNumber) = getApplicationDetailsForBooking(booking)

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        else -> null
      }

    val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
    val staffDetails = when (staffDetailsResult) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    val approvedPremises = booking.premises as ApprovedPremisesEntity

    domainEventService.saveBookingChangedEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = booking.crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = bookingChangedAt.toInstant(),
        bookingId = booking.id,
        data = BookingChangedEnvelope(
          id = domainEventId,
          timestamp = bookingChangedAt.toInstant(),
          eventType = EventType.bookingChanged,
          eventDetails = BookingChanged(
            applicationId = applicationId,
            applicationUrl = applicationUrlTemplate.replace("#id", applicationId.toString()),
            bookingId = booking.id,
            personReference = PersonReference(
              crn = booking.application?.crn ?: booking.offlineApplication!!.crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = eventNumber,
            changedAt = bookingChangedAt.toInstant(),
            changedBy = staffDetails.toStaffMember(),
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
    bedId: UUID?,
    assessmentId: UUID?,
    enableTurnarounds: Boolean,
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    val validationResult = validated {
      if (bedId == null) {
        "$.bedId" hasValidationError "empty"
        return@validated fieldValidationError
      }

      val expectedLastUnavailableDate =
        workingDayService.addWorkingDays(departureDate, premises.turnaroundWorkingDayCount)
      getBookingWithConflictingDates(arrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
      }

      getLostBedWithConflictingDates(arrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }

      bedRepository.findArchivedBedByBedIdAndDate(bedId, departureDate)?.let {
        return@validated it.id hasConflictError "BedSpace is archived from ${it.endDate} which overlaps with the desired dates"
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

      val application = when (assessmentId) {
        null -> null
        else -> {
          val result = assessmentRepository.findByIdOrNull(assessmentId)
          if (result == null) {
            "$.assessmentId" hasValidationError "doesNotExist"
          }
          result?.application
        }
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
          arrivals = mutableListOf(),
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
          application = application,
          offlineApplication = null,
          turnarounds = mutableListOf(),
          placementRequest = null,
          status = BookingStatus.provisional,
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

      cas3DomainEventService.saveBookingProvisionallyMadeEvent(booking, user)

      success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  @Transactional
  fun createTurnaround(
    booking: BookingEntity,
    workingDays: Int,
  ) = validated {
    if (workingDays < 0) {
      "$.workingDays" hasValidationError "isNotAPositiveInteger"
    }

    val expectedLastUnavailableDate = workingDayService.addWorkingDays(booking.departureDate, workingDays)
    getBookingWithConflictingDates(
      booking.arrivalDate,
      expectedLastUnavailableDate,
      booking.id,
      booking.bed!!.id,
    )?.let {
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
    booking.status = BookingStatus.arrived
    updateBooking(booking)

    if (shouldCreateDomainEventForBooking(booking, user)) {
      val domainEventId = UUID.randomUUID()

      val offenderDetails =
        when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user!!.deliusUsername, true)) {
          is AuthorisableActionResult.Success -> offenderDetailsResult.entity
          else -> null
        }

      val keyWorkerStaffDetailsResult = communityApiClient.getStaffUserDetailsForStaffCode(keyWorkerStaffCode!!)
      val keyWorkerStaffDetails = when (keyWorkerStaffDetailsResult) {
        is ClientResult.Success -> keyWorkerStaffDetailsResult.body
        is ClientResult.Failure -> keyWorkerStaffDetailsResult.throwException()
      }

      val (applicationId, eventNumber, submittedAt) = getApplicationDetailsForBooking(booking)
      val approvedPremises = booking.premises as ApprovedPremisesEntity

      domainEventService.savePersonArrivedEvent(
        emit = !arrivedAndDepartedDomainEventsDisabled,
        domainEvent = DomainEvent(
          id = domainEventId,
          applicationId = applicationId,
          crn = booking.crn,
          nomsNumber = offenderDetails?.otherIds?.nomsNumber,
          occurredAt = arrivalDateTime,
          bookingId = booking.id,
          data = PersonArrivedEnvelope(
            id = domainEventId,
            timestamp = occurredAt.toInstant(),
            eventType = EventType.personArrived,
            eventDetails = PersonArrived(
              applicationId = applicationId,
              applicationUrl = applicationUrlTemplate.replace("#id", applicationId.toString()),
              bookingId = booking.id,
              personReference = PersonReference(
                crn = booking.crn,
                noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
              ),
              deliusEventNumber = eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
              ),
              applicationSubmittedOn = submittedAt.toLocalDate(),
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
    if (booking.premises is TemporaryAccommodationPremisesEntity) {
      return generalError("CAS3 booking arrival not supported here, preferred method is createCas3Arrival")
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

  @Transactional
  fun createCas3Arrival(
    user: UserEntity? = null,
    booking: BookingEntity,
    arrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    notes: String?,
    keyWorkerStaffCode: String?,
  ) = validated<ArrivalEntity> {
    if (booking.premises !is TemporaryAccommodationPremisesEntity) {
      return generalError("CAS3 Arrivals cannot be set on non-CAS3 premise")
    }

    if (expectedDepartureDate.isBefore(arrivalDate)) {
      return "$.expectedDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }
    val isFirstArrival = booking.arrivals.isNullOrEmpty()
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

    when (isFirstArrival) {
      true -> cas3DomainEventService.savePersonArrivedEvent(booking, user)
      else -> cas3DomainEventService.savePersonArrivedUpdatedEvent(booking, user)
    }

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

    if (shouldCreateDomainEventForBooking(booking, user)) {
      val domainEventId = UUID.randomUUID()
      val user = user as UserEntity

      val offenderDetails = when (
        val offenderDetailsResult = offenderService.getOffenderByCrn(
          booking.crn,
          user.deliusUsername,
          user.hasQualification(UserQualification.LAO),
        )
      ) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Unauthorised")
        is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Not Found")
      }

      val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
      val staffDetails = when (staffDetailsResult) {
        is ClientResult.Success -> staffDetailsResult.body
        is ClientResult.Failure -> staffDetailsResult.throwException()
      }

      val (applicationId, eventNumber, _) = getApplicationDetailsForBooking(booking)
      val approvedPremises = booking.premises as ApprovedPremisesEntity

      domainEventService.savePersonNotArrivedEvent(
        emit = !arrivedAndDepartedDomainEventsDisabled,
        domainEvent = DomainEvent(
          id = domainEventId,
          applicationId = applicationId,
          crn = booking.crn,
          nomsNumber = offenderDetails.otherIds.nomsNumber,
          occurredAt = date.toLocalDateTime().toInstant(),
          bookingId = booking.id,
          data = PersonNotArrivedEnvelope(
            id = domainEventId,
            timestamp = occurredAt.toInstant(),
            eventType = EventType.personNotArrived,
            eventDetails = PersonNotArrived(
              applicationId = applicationId,
              applicationUrl = applicationUrlTemplate.replace("#id", applicationId.toString()),
              bookingId = booking.id,
              personReference = PersonReference(
                crn = booking.crn,
                noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
              ),
              deliusEventNumber = eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
              ),
              expectedArrivalOn = booking.originalArrivalDate,
              recordedBy = staffDetails.toStaffMember(),
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
    } else if (!serviceScopeMatches(reason.serviceScope, booking)) {
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

    createPlacementRequestIfBookingAppealed(reason, booking)

    val user = when (withdrawalContext.withdrawalTriggeredBy) {
      is WithdrawalTriggeredBySeedJob -> null
      is WithdrawalTriggeredByUser -> withdrawalContext.withdrawalTriggeredBy.user
    }
    if (shouldCreateDomainEventForBooking(booking, user)) {
      createCas1CancellationDomainEvent(booking, user, cancellationEntity, reason)
    }

    updateApplicationStatusOnCancellation(
      booking = booking,
      isUserRequestedWithdrawal = withdrawalContext.triggeringEntityType == WithdrawableEntityType.Booking,
    )

    val application = booking.application as ApprovedPremisesApplicationEntity?
    application?.let {
      cas1BookingEmailService.bookingWithdrawn(
        application = it,
        booking = booking,
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
  }

  private fun updateApplicationStatusOnCancellation(
    booking: BookingEntity,
    isUserRequestedWithdrawal: Boolean,
  ) {
    if (!isUserRequestedWithdrawal || booking.application == null) {
      return
    }

    val application = booking.application!!
    val bookings = bookingRepository.findAllByApplication(application)
    val anyActiveBookings = bookings.any { it.isActive() }
    if (!anyActiveBookings) {
      applicationService.updateApprovedPremisesApplicationStatus(
        application.id,
        ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
      )
    }
  }

  private fun createPlacementRequestIfBookingAppealed(
    reason: CancellationReasonEntity,
    booking: BookingEntity,
  ) {
    if (reason.id == approvedPremisesBookingAppealedCancellationReasonId && booking.placementRequest != null) {
      val placementRequest = booking.placementRequest!!
      placementRequestService.createPlacementRequest(
        source = PlacementRequestSource.APPEAL,
        placementRequirements = placementRequest.placementRequirements,
        placementDates = PlacementDates(
          expectedArrival = placementRequest.expectedArrival,
          duration = placementRequest.duration,
        ),
        notes = placementRequest.notes,
        isParole = false,
        null,
      )
    }
  }

  @SuppressWarnings("LongMethod")
  private fun createCas1CancellationDomainEvent(
    booking: BookingEntity,
    user: UserEntity?,
    cancellation: CancellationEntity,
    reason: CancellationReasonEntity,
  ) {
    val now = OffsetDateTime.now()

    val domainEventId = UUID.randomUUID()

    val offenderDetails = when (
      val offenderDetailsResult = offenderService.getOffenderByCrn(
        booking.crn,
        user!!.deliusUsername,
        user.hasQualification(UserQualification.LAO),
      )
    ) {
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Booking Cancelled Domain Event: Unauthorised")
      is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Booking Cancelled Domain Event: Not Found")
    }

    val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
    val staffDetails = when (staffDetailsResult) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    val (applicationId, eventNumber) = getApplicationDetailsForBooking(booking)

    val approvedPremises = booking.premises as ApprovedPremisesEntity

    domainEventService.saveBookingCancelledEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = booking.crn,
        nomsNumber = offenderDetails.otherIds.nomsNumber,
        occurredAt = now.toInstant(),
        bookingId = booking.id,
        schemaVersion = 2,
        data = BookingCancelledEnvelope(
          id = domainEventId,
          timestamp = now.toInstant(),
          eventType = EventType.bookingCancelled,
          eventDetails = BookingCancelled(
            applicationId = applicationId,
            applicationUrl = applicationUrlTemplate.replace("#id", applicationId.toString()),
            bookingId = booking.id,
            personReference = PersonReference(
              crn = booking.crn,
              noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = eventNumber,
            premises = Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ),
            cancelledBy = staffDetails.toStaffMember(),
            cancelledAt = cancellation.date.atTime(0, 0).toInstant(ZoneOffset.UTC),
            cancelledAtDate = cancellation.date,
            cancellationReason = reason.name,
            cancellationRecordedAt = now.toInstant(),
          ),
        ),
        metadata = mapOf(
          MetaDataName.CAS1_CANCELLATION_ID to cancellation.id.toString(),
        ),
      ),
    )
  }

  @Transactional
  fun createCas3Cancellation(
    booking: BookingEntity,
    cancelledAt: LocalDate,
    reasonId: UUID,
    notes: String?,
    user: UserEntity,
  ) = validated<CancellationEntity> {
    val reason = cancellationReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      "$.reason" hasValidationError "doesNotExist"
    } else if (!serviceScopeMatches(reason.serviceScope, booking)) {
      "$.reason" hasValidationError "incorrectCancellationReasonServiceScope"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }
    val isFirstCancellations = booking.cancellations.isNullOrEmpty()
    val cancellationEntity = cancellationRepository.save(
      CancellationEntity(
        id = UUID.randomUUID(),
        date = cancelledAt,
        reason = reason!!,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
        otherReason = null,
      ),
    )
    booking.status = BookingStatus.cancelled
    updateBooking(booking)
    booking.cancellations += cancellationEntity

    when (isFirstCancellations) {
      true -> cas3DomainEventService.saveBookingCancelledEvent(booking, user)
      else -> cas3DomainEventService.saveBookingCancelledUpdatedEvent(booking, user)
    }

    booking.application?.getLatestAssessment()?.let { applicationEntity ->
      moveTransitionalAccommodationAssessmentToReadyToPlace(user, applicationEntity, booking.id)
    }

    return success(cancellationEntity)
  }

  @Transactional
  fun createConfirmation(
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    notes: String?,
    user: UserEntity,
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

    if (booking.premises is TemporaryAccommodationPremisesEntity) {
      cas3DomainEventService.saveBookingConfirmedEvent(booking, user)
      findAndCloseCAS3Assessment(booking, user)
    }

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
  ) = when (booking.premises) {
    is ApprovedPremisesEntity ->
      createCas1Departure(user, booking, dateTime, reasonId, moveOnCategoryId, destinationProviderId, notes)

    is TemporaryAccommodationPremisesEntity ->
      createCas3Departure(booking, dateTime, reasonId, moveOnCategoryId, notes, user)

    else ->
      throw RuntimeException("Unknown premises type ${booking.premises::class.qualifiedName}")
  }

  private fun createCas1Departure(
    user: UserEntity?,
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    reasonId: UUID,
    moveOnCategoryId: UUID,
    destinationProviderId: UUID?,
    notes: String?,
  ) = validated<DepartureEntity> {
    if (booking.premises !is ApprovedPremisesEntity) {
      throw RuntimeException("Only CAS1 bookings are supported")
    }

    val occurredAt = OffsetDateTime.now()

    if (booking.departure != null) {
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

    val destinationProvider = when (destinationProviderId) {
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
    booking.status = BookingStatus.departed
    updateBooking(booking)
    booking.departures += departureEntity

    if (shouldCreateDomainEventForBooking(booking, user)) {
      val domainEventId = UUID.randomUUID()
      val user = user as UserEntity

      val offenderDetails = when (
        val offenderDetailsResult = offenderService.getOffenderByCrn(
          booking.crn,
          user.deliusUsername,
          user.hasQualification(UserQualification.LAO),
        )
      ) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Unauthorised")
        is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Booking Made Domain Event: Not Found")
      }

      val keyWorkerStaffDetailsResult = communityApiClient.getStaffUserDetailsForStaffCode(booking.keyWorkerStaffCode!!)
      val keyWorkerStaffDetails = when (keyWorkerStaffDetailsResult) {
        is ClientResult.Success -> keyWorkerStaffDetailsResult.body
        is ClientResult.Failure -> keyWorkerStaffDetailsResult.throwException()
      }

      val (applicationId, eventNumber) = getApplicationDetailsForBooking(booking)
      val approvedPremises = booking.premises as ApprovedPremisesEntity

      domainEventService.savePersonDepartedEvent(
        emit = !arrivedAndDepartedDomainEventsDisabled,
        domainEvent = DomainEvent(
          id = domainEventId,
          applicationId = applicationId,
          crn = booking.crn,
          nomsNumber = offenderDetails.otherIds.nomsNumber,
          occurredAt = dateTime.toInstant(),
          bookingId = booking.id,
          data = PersonDepartedEnvelope(
            id = domainEventId,
            timestamp = occurredAt.toInstant(),
            eventType = EventType.personDeparted,
            eventDetails = PersonDeparted(
              applicationId = applicationId,
              applicationUrl = applicationUrlTemplate.replace("#id", applicationId.toString()),
              bookingId = booking.id,
              personReference = PersonReference(
                crn = booking.crn,
                noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
              ),
              deliusEventNumber = eventNumber,
              premises = Premises(
                id = approvedPremises.id,
                name = approvedPremises.name,
                apCode = approvedPremises.apCode,
                legacyApCode = approvedPremises.qCode,
                localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
              ),
              keyWorker = StaffMember(
                staffCode = keyWorkerStaffDetails.staffCode,
                staffIdentifier = keyWorkerStaffDetails.staffIdentifier,
                forenames = keyWorkerStaffDetails.staff.forenames,
                surname = keyWorkerStaffDetails.staff.surname,
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

  private fun createCas3Departure(
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    reasonId: UUID,
    moveOnCategoryId: UUID,
    notes: String?,
    user: UserEntity?,
  ) = validated<DepartureEntity> {
    if (booking.premises !is TemporaryAccommodationPremisesEntity) {
      throw RuntimeException("Only CAS3 bookings are supported")
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

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val isFirstDeparture = booking.departures.isNullOrEmpty()

    val departureEntity = departureRepository.save(
      DepartureEntity(
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
    booking.status = BookingStatus.departed
    booking.departureDate = dateTime.toLocalDate()
    updateBooking(booking)
    booking.departures += departureEntity

    when (isFirstDeparture) {
      true -> cas3DomainEventService.savePersonDepartedEvent(booking, user)
      else -> cas3DomainEventService.savePersonDepartureUpdatedEvent(booking, user)
    }

    return success(departureEntity)
  }

  @Transactional
  fun createExtension(
    user: UserEntity?,
    booking: BookingEntity,
    newDepartureDate: LocalDate,
    notes: String?,
  ) = validated {
    val expectedLastUnavailableDate =
      workingDayService.addWorkingDays(newDepartureDate, booking.turnaround?.workingDayCount ?: 0)

    if (booking.service != ServiceName.approvedPremises.value) {
      val bedId = booking.bed?.id
        ?: throw InternalServerErrorProblem("No bed ID present on Booking: ${booking.id}")

      getBookingWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, booking.id, bedId)?.let {
        return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
      }

      getLostBedWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }
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

    if (shouldCreateDomainEventForBooking(booking, user)) {
      saveBookingChangedDomainEvent(
        booking = booking,
        user = user!!,
        bookingChangedAt = OffsetDateTime.now(),
      )
    }

    return success(extensionEntity)
  }

  private fun shouldCreateDomainEventForBooking(booking: BookingEntity, user: UserEntity?) =
    booking.service == ServiceName.approvedPremises.value && user != null && (booking.application != null || booking.offlineApplication?.eventNumber != null)

  @Transactional
  fun createDateChange(
    booking: BookingEntity,
    user: UserEntity,
    newArrivalDate: LocalDate?,
    newDepartureDate: LocalDate?,
  ) = validated {
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

    if (shouldCreateDomainEventForBooking(booking, user)) {
      saveBookingChangedDomainEvent(
        booking = booking,
        user = user,
        bookingChangedAt = OffsetDateTime.now(),
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
    get() = workingDayService.addWorkingDays(this.departureDate, this.turnaround?.workingDayCount ?: 0)

  fun getApplicationDetailsForBooking(booking: BookingEntity): Triple<UUID, String, OffsetDateTime> {
    val application = (booking.application as ApprovedPremisesApplicationEntity?)
    val offlineApplication = booking.offlineApplication

    val applicationId = application?.id ?: offlineApplication?.id as UUID
    val eventNumber = application?.eventNumber ?: offlineApplication?.eventNumber as String
    val submittedAt = application?.submittedAt ?: offlineApplication?.createdAt as OffsetDateTime

    return Triple(applicationId, eventNumber, submittedAt)
  }

  private fun findAndCloseCAS3Assessment(booking: BookingEntity, user: UserEntity) {
    booking.application?.let {
      val assessment =
        assessmentRepository.findByApplication_IdAndReallocatedAtNull(booking.application!!.id)
      if (assessment != null) {
        closeTransitionalAccommodationAssessment(assessment.id, user, booking)
      }
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun moveTransitionalAccommodationAssessmentToReadyToPlace(
    user: UserEntity,
    applicationEntity: AssessmentEntity,
    bookingId: UUID,
  ) {
    try {
      extractEntityFromNestedAuthorisableValidatableActionResult(
        assessmentService.acceptAssessment(
          user,
          applicationEntity.id,
          applicationEntity.document,
          null,
          null,
          null,
          "Automatically moved to ready-to-place after booking is cancelled",
        ),
      )
    } catch (exception: Exception) {
      log.error("Unable to move CAS3 assessment ${applicationEntity.id} to ready-to-place queue for $bookingId ")
      Sentry.captureException(
        RuntimeException(
          "Unable to move CAS3 assessment ${applicationEntity.id} for ready-to-place queue for $bookingId ",
          exception,
        ),
      )
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun closeTransitionalAccommodationAssessment(
    assessmentId: UUID,
    user: UserEntity,
    booking: BookingEntity,
  ) {
    try {
      extractEntityFromNestedAuthorisableValidatableActionResult(assessmentService.closeAssessment(user, assessmentId))
    } catch (exception: Exception) {
      log.error("Unable to close CAS3 assessment $assessmentId for booking ${booking.id} ", exception)
      Sentry.captureException(RuntimeException("Unable to close CAS3 assessment $assessmentId for booking ${booking.id} ", exception))
    }
  }
}

sealed interface GetBookingForPremisesResult {
  data class Success(val booking: BookingEntity) : GetBookingForPremisesResult
  object PremisesNotFound : GetBookingForPremisesResult
  object BookingNotFound : GetBookingForPremisesResult
}
