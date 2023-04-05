package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewNonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Nonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotImplementedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExtensionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoomTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
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
) : PremisesApiDelegate {

  override fun premisesSummaryGet(xServiceName: ServiceName): ResponseEntity<List<PremisesSummary>> {
    val premises = premisesService.getAllPremisesSummary(xServiceName!!)
    return ResponseEntity.ok(
      premises.map {
        premisesSummaryTransformer.transformDomainToApi(it)
      }
    )
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
        body.pdu,
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
      }
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
        pdu = body.pdu,
      )
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
      premises.bookings.map {
        val offenderResult = offenderService.getOffenderByCrn(it.crn, user.deliusUsername)

        if (offenderResult !is AuthorisableActionResult.Success) {
          throw InternalServerErrorProblem("Unable to get Person via crn: ${it.crn}")
        }

        if (offenderResult.entity.otherIds.nomsNumber == null) {
          throw InternalServerErrorProblem("No nomsNumber present for CRN")
        }

        val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(offenderResult.entity.otherIds.nomsNumber)

        if (inmateDetailResult !is AuthorisableActionResult.Success) {
          throw InternalServerErrorProblem("Unable to get InmateDetail via crn: ${it.crn}")
        }

        val staffMember = it.keyWorkerStaffCode?.let { keyWorkerStaffCode ->
          // TODO: Bookings will need to be specialised in a similar way to Premises so that TA Bookings do not have a keyWorkerStaffCode field
          if (premises !is ApprovedPremisesEntity) throw RuntimeException("Booking ${it.id} has a Key Worker specified but Premises ${premises.id} is not an ApprovedPremises")

          val staffMemberResult = staffMemberService.getStaffMemberByCode(keyWorkerStaffCode, premises.qCode)

          if (staffMemberResult !is AuthorisableActionResult.Success) {
            throw InternalServerErrorProblem("Unable to get Key Worker via Staff Code: $keyWorkerStaffCode / Q Code: ${premises.qCode}")
          }

          staffMemberResult.entity
        }

        bookingTransformer.transformJpaToApi(it, offenderResult.entity, inmateDetailResult.entity, staffMember)
      }
    )
  }

  override fun premisesPremisesIdBookingsBookingIdGet(premisesId: UUID, bookingId: UUID): ResponseEntity<Booking> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val offenderResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername)

    if (offenderResult !is AuthorisableActionResult.Success) {
      throw InternalServerErrorProblem("Unable to get Person via crn: ${booking.crn}")
    }

    if (offenderResult.entity.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No nomsNumber present for CRN")
    }

    val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(offenderResult.entity.otherIds.nomsNumber)

    if (inmateDetailResult !is AuthorisableActionResult.Success) {
      throw InternalServerErrorProblem("Unable to get InmateDetail via crn: ${booking.crn}")
    }

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

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(booking, offenderResult.entity, inmateDetailResult.entity, staffMember))
  }

  @Transactional
  override fun premisesPremisesIdBookingsPost(premisesId: UUID, body: NewBooking): ResponseEntity<Booking> {
    val user = usersService.getUserForRequest()

    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val offenderResult = offenderService.getOffenderByCrn(body.crn, user.deliusUsername)
    val offender = when (offenderResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw BadRequestProblem(mapOf("$.crn" to "doesNotExist"))
      is AuthorisableActionResult.Success -> offenderResult.entity
    }

    if (offender.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No nomsNumber present for CRN")
    }

    val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(offender.otherIds.nomsNumber)
    val inmate = when (inmateDetailResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw BadRequestProblem(mapOf("$.crn" to "doesNotExist"))
      is AuthorisableActionResult.Success -> inmateDetailResult.entity
    }

    val authorisableResult = when (premises) {
      is ApprovedPremisesEntity -> {
        bookingService.createApprovedPremisesAdHocBooking(
          user = user,
          crn = body.crn,
          arrivalDate = body.arrivalDate,
          departureDate = body.departureDate,
          bedId = body.bedId
        )
      }

      is TemporaryAccommodationPremisesEntity -> {
        bookingService.createTemporaryAccommodationBooking(
          user = user,
          premises = premises,
          crn = body.crn,
          arrivalDate = body.arrivalDate,
          departureDate = body.departureDate,
          bedId = body.bedId
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

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(createdBooking, offender, inmate, null))
  }

  override fun premisesPremisesIdBookingsBookingIdArrivalsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewArrival
  ): ResponseEntity<Arrival> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val bedId = booking.bed?.id
      ?: throw InternalServerErrorProblem("No bed ID present on Booking: $bookingId")

    throwIfBookingDatesConflict(body.arrivalDate, body.expectedDepartureDate, bookingId, bedId)
    throwIfLostBedDatesConflict(body.arrivalDate, body.expectedDepartureDate, null, bedId)

    val result = bookingService.createArrival(
      booking = booking,
      arrivalDate = body.arrivalDate,
      expectedDepartureDate = body.expectedDepartureDate,
      notes = body.notes,
      keyWorkerStaffCode = body.keyWorkerStaffCode,
      user = user
    )

    val arrival = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(arrivalTransformer.transformJpaToApi(arrival))
  }

  override fun premisesPremisesIdBookingsBookingIdNonArrivalsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewNonarrival
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
      notes = body.notes
    )

    val nonArrivalEntity = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(nonArrivalTransformer.transformJpaToApi(nonArrivalEntity))
  }

  override fun premisesPremisesIdBookingsBookingIdCancellationsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewCancellation
  ): ResponseEntity<Cancellation> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createCancellation(
      booking = booking,
      date = body.date,
      reasonId = body.reason,
      notes = body.notes
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
    body: NewDeparture
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
      notes = body.notes
    )

    val departure = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(departureTransformer.transformJpaToApi(departure))
  }

  override fun premisesPremisesIdBookingsBookingIdExtensionsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewExtension
  ): ResponseEntity<Extension> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val bedId = booking.bed?.id
      ?: throw InternalServerErrorProblem("No bed ID present on Booking: $bookingId")

    throwIfBookingDatesConflict(booking.arrivalDate, body.newDepartureDate, bookingId, bedId)
    throwIfLostBedDatesConflict(booking.arrivalDate, body.newDepartureDate, null, bedId)

    val result = bookingService.createExtension(
      booking = booking,
      newDepartureDate = body.newDepartureDate,
      notes = body.notes
    )

    val extension = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(extensionTransformer.transformJpaToApi(extension))
  }

  override fun premisesPremisesIdLostBedsPost(premisesId: UUID, body: NewLostBed): ResponseEntity<LostBed> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    throwIfBookingDatesConflict(body.startDate, body.endDate, null, body.bedId)
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

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(premises.lostBeds.map(lostBedsTransformer::transformJpaToApi))
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

    if (premises is ApprovedPremisesEntity && !user.hasAnyRole(UserRole.MANAGER, UserRole.MATCHER)) {
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
        body.notes
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
        lastLostBedsDate ?: LocalDate.now()
      )
    )

    return ResponseEntity.ok(
      capacityForPeriod.map {
        DateCapacity(
          date = it.key,
          availableBeds = it.value.getFreeCapacity(premises.totalBeds)
        )
      }
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
      roomService.createRoom(premises, newRoom.name, newRoom.notes, newRoom.characteristicIds)
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
    updateRoom: UpdateRoom
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
    bedId: UUID
  ) {
    bookingService.getBookingWithConflictingDates(arrivalDate, departureDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(it.id, "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates")
    }
  }

  private fun throwIfLostBedDatesConflict(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID
  ) {
    bookingService.getLostBedWithConflictingDates(startDate, endDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(it.id, "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates")
    }
  }
}
