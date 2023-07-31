package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import arrow.core.Ior
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedOccupancyRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBedMove
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewNonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTurnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Nonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotImplementedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CalendarService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LostBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CalendarTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DateChangeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExtensionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoomTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TurnaroundTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromAuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPersonDetailsForCrn
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.transaction.Transactional

@Service
class PremisesController(
  private val usersService: UserService,
  private val userAccessService: UserAccessService,
  private val premisesService: PremisesService,
  private val offenderService: OffenderService,
  private val bookingService: BookingService,
  private val lostBedsService: LostBedService,
  private val bedService: BedService,
  private val calendarService: CalendarService,
  private val premisesTransformer: PremisesTransformer,
  private val premisesSummaryTransformer: PremisesSummaryTransformer,
  private val bookingTransformer: BookingTransformer,
  private val lostBedsTransformer: LostBedsTransformer,
  private val arrivalTransformer: ArrivalTransformer,
  private val nonArrivalTransformer: NonArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val confirmationTransformer: ConfirmationTransformer,
  private val departureTransformer: DepartureTransformer,
  private val extensionTransformer: ExtensionTransformer,
  private val staffMemberTransformer: StaffMemberTransformer,
  private val staffMemberService: StaffMemberService,
  private val roomService: RoomService,
  private val roomTransformer: RoomTransformer,
  private val lostBedCancellationTransformer: LostBedCancellationTransformer,
  private val turnaroundTransformer: TurnaroundTransformer,
  private val bedSummaryTransformer: BedSummaryTransformer,
  private val bedDetailTransformer: BedDetailTransformer,
  private val calendarTransformer: CalendarTransformer,
  private val dateChangeTransformer: DateChangeTransformer,
) : PremisesApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun premisesSummaryGet(xServiceName: ServiceName): ResponseEntity<List<PremisesSummary>> {
    val transformedSummaries = when (xServiceName) {
      ServiceName.approvedPremises -> {
        val summaries = premisesService.getAllApprovedPremisesSummaries()

        summaries.map(premisesSummaryTransformer::transformDomainToApi)
      }
      ServiceName.cas2 -> throw RuntimeException("CAS2 not supported")
      ServiceName.temporaryAccommodation -> {
        val user = usersService.getUserForRequest()
        val summaries = premisesService.getAllTemporaryAccommodationPremisesSummaries(user.probationRegion.id)

        summaries.map(premisesSummaryTransformer::transformDomainToApi)
      }
    }

    return ResponseEntity.ok(transformedSummaries)
  }
  override fun premisesPremisesIdPut(premisesId: UUID, body: UpdatePremises): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises) || !userAccessService.currentUserCanAccessRegion(body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val updatePremisesResult = premisesService
      .updatePremises(
        premisesId,
        body.addressLine1,
        body.addressLine2,
        body.town,
        body.postcode,
        body.localAuthorityAreaId,
        body.probationRegionId,
        body.characteristicIds,
        body.notes,
        body.status,
        Ior.fromNullables(body.pdu, body.probationDeliveryUnitId)?.toEither(),
        body.turnaroundWorkingDayCount,
      )

    val validationResult = when (updatePremisesResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(premisesId, "Premises")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> updatePremisesResult.entity
    }

    val updatedPremises = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(updatedPremises, updatedPremises.totalBeds))
  }

  override fun premisesGet(xServiceName: ServiceName?, xUserRegion: UUID?): ResponseEntity<List<Premises>> {
    if (!userAccessService.currentUserCanAccessRegion(xUserRegion)) {
      throw ForbiddenProblem()
    }

    val premises = when {
      xServiceName == null && xUserRegion == null -> premisesService.getAllPremises()
      xServiceName != null && xUserRegion != null -> premisesService.getAllPremisesInRegionForService(xUserRegion, xServiceName)
      xServiceName != null -> premisesService.getAllPremisesForService(xServiceName)
      else -> premisesService.getAllPremisesInRegion(xUserRegion!!)
    }

    return ResponseEntity.ok(
      premises.map {
        val availableBedsForToday = premisesService.getAvailabilityForRange(it, LocalDate.now(), LocalDate.now().plusDays(1))
          .values.first().getFreeCapacity(it.totalBeds)

        premisesTransformer.transformJpaToApi(it, availableBedsForToday)
      },
    )
  }

  override fun premisesPost(body: NewPremises, xServiceName: ServiceName?): ResponseEntity<Premises> {
    if (!userAccessService.currentUserCanAccessRegion(body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val serviceName = when (xServiceName == null) {
      true -> ServiceName.approvedPremises.value
      false -> xServiceName.value
    }

    val premises = extractResultEntityOrThrow(
      premisesService.createNewPremises(
        addressLine1 = body.addressLine1,
        addressLine2 = body.addressLine2,
        town = body.town,
        postcode = body.postcode,
        latitude = null,
        longitude = null,
        service = serviceName,
        localAuthorityAreaId = body.localAuthorityAreaId,
        probationRegionId = body.probationRegionId,
        name = body.name,
        notes = body.notes,
        characteristicIds = body.characteristicIds,
        status = body.status,
        probationDeliveryUnitIdentifier = Ior.fromNullables(body.pdu, body.probationDeliveryUnitId)?.toEither(),
        turnaroundWorkingDayCount = body.turnaroundWorkingDayCount,
      ),
    )
    return ResponseEntity(premisesTransformer.transformJpaToApi(premises, premises.totalBeds), HttpStatus.CREATED)
  }

  override fun premisesPremisesIdGet(premisesId: UUID): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val availableBedsForToday = premisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
      .values.first().getFreeCapacity(premises.totalBeds)

    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(premises, availableBedsForToday))
  }

  override fun premisesPremisesIdBookingsGet(premisesId: UUID): ResponseEntity<List<Booking>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(
      premises.bookings.mapNotNull {
        val personDetails = getPersonDetailsForCrn(log, it.crn, user.deliusUsername, offenderService, user.hasQualification(UserQualification.LAO))

        if (personDetails == null) {
          log.warn("Unable to get Person via crn: ${it.crn}")
          return@mapNotNull null
        }

        val (offenderDetails, inmateDetails) = personDetails

        val staffMember = it.keyWorkerStaffCode?.let { keyWorkerStaffCode ->
          // TODO: Bookings will need to be specialised in a similar way to Premises so that TA Bookings do not have a keyWorkerStaffCode field
          if (premises !is ApprovedPremisesEntity) throw RuntimeException("Booking ${it.id} has a Key Worker specified but Premises ${premises.id} is not an ApprovedPremises")

          val staffMemberResult = staffMemberService.getStaffMemberByCode(keyWorkerStaffCode, premises.qCode)

          if (staffMemberResult !is AuthorisableActionResult.Success) {
            throw InternalServerErrorProblem("Unable to get Key Worker via Staff Code: $keyWorkerStaffCode / Q Code: ${premises.qCode}")
          }

          staffMemberResult.entity
        }

        bookingTransformer.transformJpaToApi(it, offenderDetails, inmateDetails, staffMember)
      },
    )
  }

  override fun premisesPremisesIdBookingsBookingIdGet(premisesId: UUID, bookingId: UUID): ResponseEntity<Booking> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val personDetails = getPersonDetailsForCrn(log, booking.crn, user.deliusUsername, offenderService, user.hasQualification(UserQualification.LAO))
      ?: throw InternalServerErrorProblem("Unable to get Person via crn: ${booking.crn}")

    val (offenderDetails, inmateDetails) = personDetails

    val staffMember = booking.keyWorkerStaffCode?.let { keyWorkerStaffCode ->
      val premises = booking.premises

      // TODO: Bookings will need to be specialised in a similar way to Premises so that TA Bookings do not have a keyWorkerStaffCode field
      if (premises !is ApprovedPremisesEntity) throw RuntimeException("Booking has a Key Worker specified but Premises is not an ApprovedPremises")

      val staffMemberResult = staffMemberService.getStaffMemberByCode(keyWorkerStaffCode, premises.qCode)

      if (staffMemberResult !is AuthorisableActionResult.Success) {
        throw InternalServerErrorProblem("Unable to get Key Worker via Staff Code: $keyWorkerStaffCode / Q Code: ${premises.qCode}")
      }

      staffMemberResult.entity
    }

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(booking, offenderDetails, inmateDetails, staffMember))
  }

  @Transactional
  override fun premisesPremisesIdBookingsPost(premisesId: UUID, body: NewBooking): ResponseEntity<Booking> {
    val user = usersService.getUserForRequest()

    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val personDetails = getPersonDetailsForCrn(log, body.crn, user.deliusUsername, offenderService, user.hasQualification(UserQualification.LAO))
      ?: throw InternalServerErrorProblem("Unable to get Person via crn: ${body.crn}")

    val (offenderDetails, inmateDetails) = personDetails

    val authorisableResult = when (premises) {
      is ApprovedPremisesEntity -> {
        bookingService.createApprovedPremisesAdHocBooking(
          user = user,
          crn = body.crn,
          nomsNumber = inmateDetails?.offenderNo,
          arrivalDate = body.arrivalDate,
          departureDate = body.departureDate,
          bedId = body.bedId,
        )
      }

      is TemporaryAccommodationPremisesEntity -> {
        bookingService.createTemporaryAccommodationBooking(
          user = user,
          premises = premises,
          crn = body.crn,
          nomsNumber = inmateDetails?.offenderNo,
          arrivalDate = body.arrivalDate,
          departureDate = body.departureDate,
          bedId = body.bedId,
          enableTurnarounds = body.enableTurnarounds ?: false,
        )
      }

      else -> throw RuntimeException("Unsupported New Booking type: ${body::class.qualifiedName}")
    }

    val validatableResult = when (authorisableResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(body.crn, "Offender")
      is AuthorisableActionResult.Success -> authorisableResult.entity
    }

    val createdBooking = when (validatableResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validatableResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validatableResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validatableResult.conflictingEntityId, conflictReason = validatableResult.message)
      is ValidatableActionResult.Success -> validatableResult.entity
    }

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(createdBooking, offenderDetails, inmateDetails, null))
  }

  override fun premisesPremisesIdBookingsBookingIdArrivalsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewArrival,
  ): ResponseEntity<Arrival> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = when (body) {
      is NewCas1Arrival -> {
        val bedId = booking.bed?.id
        val arrivalDate = LocalDate.from(body.arrivalDateTime.atZone(ZoneOffset.UTC))

        if (bedId != null) {
          throwIfLostBedDatesConflict(arrivalDate, body.expectedDepartureDate, null, bedId)
        }

        bookingService.createCas1Arrival(
          booking = booking,
          arrivalDateTime = body.arrivalDateTime,
          expectedDepartureDate = body.expectedDepartureDate,
          notes = body.notes,
          keyWorkerStaffCode = body.keyWorkerStaffCode,
          user = user,
        )
      }
      is NewCas2Arrival -> {
        val bedId = booking.bed?.id
          ?: throw InternalServerErrorProblem("No bed ID present on Booking: $bookingId")

        throwIfBookingDatesConflict(body.arrivalDate, body.expectedDepartureDate, bookingId, bedId)
        throwIfLostBedDatesConflict(body.arrivalDate, body.expectedDepartureDate, null, bedId)

        bookingService.createArrival(
          booking = booking,
          arrivalDate = body.arrivalDate,
          expectedDepartureDate = body.expectedDepartureDate,
          notes = body.notes,
          keyWorkerStaffCode = body.keyWorkerStaffCode,
          user = user,
        )
      }
      is NewCas3Arrival -> {
        val bedId = booking.bed?.id
          ?: throw InternalServerErrorProblem("No bed ID present on Booking: $bookingId")

        throwIfBookingDatesConflict(body.arrivalDate, body.expectedDepartureDate, bookingId, bedId)
        throwIfLostBedDatesConflict(body.arrivalDate, body.expectedDepartureDate, null, bedId)

        bookingService.createArrival(
          booking = booking,
          arrivalDate = body.arrivalDate,
          expectedDepartureDate = body.expectedDepartureDate,
          notes = body.notes,
          keyWorkerStaffCode = body.keyWorkerStaffCode,
          user = user,
        )
      }
      else -> throw RuntimeException("Unsupported NewArrival type: ${body::class.qualifiedName}")
    }

    val arrival = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(arrivalTransformer.transformJpaToApi(arrival))
  }

  override fun premisesPremisesIdBookingsBookingIdMovesPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewBedMove,
  ): ResponseEntity<Unit> {
    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    extractEntityFromValidatableActionResult(
      extractEntityFromAuthorisableActionResult(
        bookingService.moveBooking(booking, body.bedId, body.notes, user),
      ),
    )

    return ResponseEntity.ok(Unit)
  }

  override fun premisesPremisesIdBookingsBookingIdNonArrivalsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewNonarrival,
  ): ResponseEntity<Nonarrival> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createNonArrival(
      user = user,
      booking = booking,
      date = body.date,
      reasonId = body.reason,
      notes = body.notes,
    )

    val nonArrivalEntity = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(nonArrivalTransformer.transformJpaToApi(nonArrivalEntity))
  }

  override fun premisesPremisesIdBookingsBookingIdCancellationsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewCancellation,
  ): ResponseEntity<Cancellation> {
    val user = usersService.getUserForRequest()

    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createCancellation(
      user = user,
      booking = booking,
      cancelledAt = body.date,
      reasonId = body.reason,
      notes = body.notes,
    )

    val cancellation = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(cancellationTransformer.transformJpaToApi(cancellation))
  }

  override fun premisesPremisesIdBookingsBookingIdConfirmationsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewConfirmation,
  ): ResponseEntity<Confirmation> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createConfirmation(
      booking = booking,
      dateTime = OffsetDateTime.now(),
      notes = body.notes,
    )

    val confirmation = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(confirmationTransformer.transformJpaToApi(confirmation))
  }

  override fun premisesPremisesIdBookingsBookingIdDeparturesPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewDeparture,
  ): ResponseEntity<Departure> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createDeparture(
      user = user,
      booking = booking,
      dateTime = body.dateTime.atOffset(ZoneOffset.UTC),
      reasonId = body.reasonId,
      moveOnCategoryId = body.moveOnCategoryId,
      destinationProviderId = body.destinationProviderId,
      notes = body.notes,
    )

    val departure = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(departureTransformer.transformJpaToApi(departure))
  }

  override fun premisesPremisesIdBookingsBookingIdExtensionsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewExtension,
  ): ResponseEntity<Extension> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createExtension(
      user = usersService.getUserForRequest(),
      booking = booking,
      newDepartureDate = body.newDepartureDate,
      notes = body.notes,
    )

    val extension = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(extensionTransformer.transformJpaToApi(extension))
  }

  @Transactional
  override fun premisesPremisesIdBookingsBookingIdDateChangesPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewDateChange,
  ): ResponseEntity<DateChange> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)
    val user = usersService.getUserForRequest()

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createDateChange(
      booking = booking,
      user = user,
      newArrivalDate = body.newArrivalDate,
      newDepartureDate = body.newDepartureDate,
    )

    val dateChange = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(dateChangeTransformer.transformJpaToApi(dateChange))
  }

  override fun premisesPremisesIdLostBedsPost(premisesId: UUID, body: NewLostBed): ResponseEntity<LostBed> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    if (premises !is ApprovedPremisesEntity) {
      throwIfBookingDatesConflict(body.startDate, body.endDate, null, body.bedId)
    }

    throwIfLostBedDatesConflict(body.startDate, body.endDate, null, body.bedId)

    val result = premisesService.createLostBeds(
      premises = premises,
      startDate = body.startDate,
      endDate = body.endDate,
      reasonId = body.reason,
      referenceNumber = body.referenceNumber,
      notes = body.notes,
      bedId = body.bedId,
    )

    val lostBeds = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(lostBeds))
  }

  override fun premisesPremisesIdLostBedsGet(premisesId: UUID): ResponseEntity<List<LostBed>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val lostBeds = lostBedsService.getActiveLostBedsForPremisesId(premisesId)

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(lostBeds.map(lostBedsTransformer::transformJpaToApi))
  }

  override fun premisesPremisesIdLostBedsLostBedIdGet(premisesId: UUID, lostBedId: UUID): ResponseEntity<LostBed> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    val lostBed = premises.lostBeds.firstOrNull { it.id == lostBedId }
      ?: throw NotFoundProblem(lostBedId, "LostBed")

    val user = usersService.getUserForRequest()

    if (premises is ApprovedPremisesEntity && !user.hasAnyRole(UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(lostBed))
  }

  override fun premisesPremisesIdLostBedsLostBedIdPut(
    premisesId: UUID,
    lostBedId: UUID,
    body: UpdateLostBed,
  ): ResponseEntity<LostBed> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    val lostBed = premises.lostBeds.firstOrNull { it.id == lostBedId } ?: throw NotFoundProblem(lostBedId, "LostBed")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    throwIfBookingDatesConflict(body.startDate, body.endDate, null, lostBed.bed.id)
    throwIfLostBedDatesConflict(body.startDate, body.endDate, lostBedId, lostBed.bed.id)

    val updateLostBedResult = premisesService
      .updateLostBeds(
        lostBedId,
        body.startDate,
        body.endDate,
        body.reason,
        body.referenceNumber,
        body.notes,
      )

    val validationResult = when (updateLostBedResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(lostBedId, "LostBed")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> updateLostBedResult.entity
    }

    val updatedLostBed = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(updatedLostBed))
  }

  override fun premisesPremisesIdLostBedsLostBedIdCancellationsPost(
    premisesId: UUID,
    lostBedId: UUID,
    body: NewLostBedCancellation,
  ): ResponseEntity<LostBedCancellation> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    val lostBed = premises.lostBeds.firstOrNull { it.id == lostBedId } ?: throw NotFoundProblem(lostBedId, "LostBed")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    val cancelLostBedResult = premisesService.cancelLostBed(
      lostBed = lostBed,
      notes = body.notes,
    )

    val cancellation = when (cancelLostBedResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = cancelLostBedResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = cancelLostBedResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = cancelLostBedResult.conflictingEntityId, conflictReason = cancelLostBedResult.message)
      is ValidatableActionResult.Success -> cancelLostBedResult.entity
    }

    return ResponseEntity.ok(lostBedCancellationTransformer.transformJpaToApi(cancellation))
  }

  override fun premisesPremisesIdCapacityGet(premisesId: UUID): ResponseEntity<List<DateCapacity>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremisesCapacity(premises)) {
      throw ForbiddenProblem()
    }

    val lastBookingDate = premisesService.getLastBookingDate(premises)
    val lastLostBedsDate = premisesService.getLastLostBedsDate(premises)

    val capacityForPeriod = premisesService.getAvailabilityForRange(
      premises,
      LocalDate.now(),
      maxOf(
        LocalDate.now(),
        lastBookingDate ?: LocalDate.now(),
        lastLostBedsDate ?: LocalDate.now(),
      ),
    )

    return ResponseEntity.ok(
      capacityForPeriod.map {
        DateCapacity(
          date = it.key,
          availableBeds = it.value.getFreeCapacity(premises.totalBeds),
        )
      },
    )
  }

  override fun premisesPremisesIdStaffGet(premisesId: UUID): ResponseEntity<List<StaffMember>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremisesStaff(premises)) {
      throw ForbiddenProblem()
    }

    if (premises !is ApprovedPremisesEntity) {
      throw NotImplementedProblem("Fetching staff for non-AP Premises is not currently supported")
    }

    val staffMembersResult = staffMemberService.getStaffMembersForQCode(premises.qCode)

    val staffMembers = when (staffMembersResult) {
      is AuthorisableActionResult.Success -> staffMembersResult.entity
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw InternalServerErrorProblem("No team found for QCode: ${premises.qCode}")
    }

    return ResponseEntity.ok(staffMembers.content.map(staffMemberTransformer::transformDomainToApi))
  }

  override fun premisesPremisesIdRoomsGet(premisesId: UUID): ResponseEntity<List<Room>> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(premises.rooms.map(roomTransformer::transformJpaToApi))
  }

  override fun premisesPremisesIdRoomsPost(premisesId: UUID, newRoom: NewRoom): ResponseEntity<Room> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val room = extractResultEntityOrThrow(
      roomService.createRoom(premises, newRoom.name, newRoom.notes, newRoom.characteristicIds),
    )

    return ResponseEntity(roomTransformer.transformJpaToApi(room), HttpStatus.CREATED)
  }

  override fun premisesPremisesIdRoomsRoomIdGet(premisesId: UUID, roomId: UUID): ResponseEntity<Room> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val room = premises.rooms.find { it.id == roomId } ?: throw NotFoundProblem(roomId, "Room")

    return ResponseEntity.ok(roomTransformer.transformJpaToApi(room))
  }

  override fun premisesPremisesIdRoomsRoomIdPut(
    premisesId: UUID,
    roomId: UUID,
    updateRoom: UpdateRoom,
  ): ResponseEntity<Room> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val updateRoomResult = roomService.updateRoom(premises, roomId, updateRoom.notes, updateRoom.characteristicIds)

    val validationResult = when (updateRoomResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(roomId, "Room")
      is AuthorisableActionResult.Success -> updateRoomResult.entity
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    val room = extractResultEntityOrThrow(validationResult)

    return ResponseEntity.ok(roomTransformer.transformJpaToApi(room))
  }

  override fun premisesPremisesIdBookingsBookingIdTurnaroundsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewTurnaround,
  ): ResponseEntity<Turnaround> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createTurnaround(booking, body.workingDays)
    val turnaround = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(turnaroundTransformer.transformJpaToApi(turnaround))
  }

  override fun premisesPremisesIdBedsGet(premisesId: UUID): ResponseEntity<List<BedSummary>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(premisesService.getBeds(premisesId).map(bedSummaryTransformer::transformToApi))
  }

  override fun premisesPremisesIdBedsBedIdGet(premisesId: UUID, bedId: UUID): ResponseEntity<BedDetail> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val validationResult = when (val bedResult = bedService.getBedAndRoomCharacteristics(bedId)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(bedId, "Bed")
      is AuthorisableActionResult.Success -> bedResult.entity
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(bedDetailTransformer.transformToApi(validationResult))
  }

  override fun premisesPremisesIdCalendarGet(premisesId: UUID, startDate: LocalDate, endDate: LocalDate): ResponseEntity<List<BedOccupancyRange>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    if (startDate >= endDate) {
      throw BadRequestProblem(errorDetail = "startDate must be before endDate")
    }

    val calendarResult = calendarService.getCalendarInfo(
      user = user,
      premisesId = premises.id,
      startDate = startDate,
      endDate = endDate,
    )

    val transformedResult = calendarTransformer.transformDomainToApi(startDate, endDate, calendarResult)

    return ResponseEntity(transformedResult, HttpStatus.OK)
  }

  private fun getBookingForPremisesOrThrow(premisesId: UUID, bookingId: UUID) = when (val result = bookingService.getBookingForPremises(premisesId, bookingId)) {
    is GetBookingForPremisesResult.Success -> result.booking
    is GetBookingForPremisesResult.PremisesNotFound -> throw NotFoundProblem(premisesId, "Premises")
    is GetBookingForPremisesResult.BookingNotFound -> throw NotFoundProblem(bookingId, "Booking")
  }

  private fun <EntityType> extractResultEntityOrThrow(result: ValidatableActionResult<EntityType>) = when (result) {
    is ValidatableActionResult.Success -> result.entity
    is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = result.message)
    is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = result.validationMessages)
    is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = result.conflictingEntityId, conflictReason = result.message)
  }

  private fun throwIfBookingDatesConflict(
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) {
    bookingService.getBookingWithConflictingDates(arrivalDate, departureDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(it.id, "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates")
    }
  }

  private fun throwIfLostBedDatesConflict(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) {
    bookingService.getLostBedWithConflictingDates(startDate, endDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(it.id, "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates")
    }
  }
}
