package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewNonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Nonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotImplementedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExtensionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoomTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import java.time.LocalDate
import java.util.UUID

@Service
class PremisesController(
  private val httpAuthService: HttpAuthService,
  private val premisesService: PremisesService,
  private val offenderService: OffenderService,
  private val bookingService: BookingService,
  private val premisesTransformer: PremisesTransformer,
  private val bookingTransformer: BookingTransformer,
  private val lostBedsTransformer: LostBedsTransformer,
  private val arrivalTransformer: ArrivalTransformer,
  private val nonArrivalTransformer: NonArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val departureTransformer: DepartureTransformer,
  private val extensionTransformer: ExtensionTransformer,
  private val staffMemberTransformer: StaffMemberTransformer,
  private val staffMemberService: StaffMemberService,
  private val roomService: RoomService,
  private val roomTransformer: RoomTransformer,
) : PremisesApiDelegate {
  override fun premisesGet(xServiceName: ServiceName?): ResponseEntity<List<Premises>> {
    val premises = if (xServiceName == null) {
      premisesService.getAllPremises()
    } else {
      premisesService.getAllPremisesForService(xServiceName)
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

    val serviceName = when (xServiceName == null) {
      true -> ServiceName.approvedPremises.value
      false -> xServiceName.value
    }

    val premises = extractResultEntityOrThrow(
      premisesService.createNewPremises(
        addressLine1 = body.addressLine1,
        postcode = body.postcode,
        service = serviceName,
        localAuthorityAreaId = body.localAuthorityAreaId,
        name = body.name,
        notes = body.notes
      )
    )
    return ResponseEntity(premisesTransformer.transformJpaToApi(premises, premises.totalBeds), HttpStatus.CREATED)
  }

  override fun premisesPremisesIdGet(premisesId: UUID): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val availableBedsForToday = premisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
      .values.first().getFreeCapacity(premises.totalBeds)

    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(premises, availableBedsForToday))
  }

  override fun premisesPremisesIdBookingsGet(premisesId: UUID): ResponseEntity<List<Booking>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    return ResponseEntity.ok(
      premises.bookings.map {
        val offenderResult = offenderService.getOffenderByCrn(it.crn, username)

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

        val staffMember = it.keyWorkerStaffId?.let { keyWorkerStaffId ->
          val staffMemberResult = staffMemberService.getStaffMemberById(keyWorkerStaffId)

          if (staffMemberResult !is AuthorisableActionResult.Success) {
            throw InternalServerErrorProblem("Unable to get Key Worker via Staff Id: $keyWorkerStaffId")
          }

          staffMemberResult.entity
        }

        bookingTransformer.transformJpaToApi(it, offenderResult.entity, inmateDetailResult.entity, staffMember)
      }
    )
  }

  override fun premisesPremisesIdBookingsBookingIdGet(premisesId: UUID, bookingId: UUID): ResponseEntity<Booking> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val offenderResult = offenderService.getOffenderByCrn(booking.crn, httpAuthService.getDeliusPrincipalOrThrow().name)

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

    val staffMember = booking.keyWorkerStaffId?.let { keyWorkerStaffId ->
      val staffMemberResult = staffMemberService.getStaffMemberById(keyWorkerStaffId)

      if (staffMemberResult !is AuthorisableActionResult.Success) {
        throw InternalServerErrorProblem("Unable to get Key Worker via Staff Id: $keyWorkerStaffId")
      }

      staffMemberResult.entity
    }

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(booking, offenderResult.entity, inmateDetailResult.entity, staffMember))
  }

  override fun premisesPremisesIdBookingsPost(premisesId: UUID, body: NewBooking): ResponseEntity<Booking> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val validationErrors = ValidationErrors()

    val offenderResult = offenderService.getOffenderByCrn(body.crn, httpAuthService.getDeliusPrincipalOrThrow().name)
    if (offenderResult is AuthorisableActionResult.Unauthorised) throw ForbiddenProblem()
    if (offenderResult is AuthorisableActionResult.NotFound) throw BadRequestProblem(mapOf("crn" to "Invalid crn"))
    offenderResult as AuthorisableActionResult.Success

    if (offenderResult.entity.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No nomsNumber present for CRN")
    }

    val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(offenderResult.entity.otherIds.nomsNumber)
    if (offenderResult is AuthorisableActionResult.Unauthorised) throw ForbiddenProblem()
    if (offenderResult is AuthorisableActionResult.NotFound) validationErrors["crn"] = "Invalid crn"
    inmateDetailResult as AuthorisableActionResult.Success

    if (validationErrors.any()) {
      throw BadRequestProblem(validationErrors)
    }

    val booking = bookingService.createBooking(
      BookingEntity(
        id = UUID.randomUUID(),
        crn = offenderResult.entity.otherIds.crn,
        arrivalDate = body.arrivalDate,
        departureDate = body.departureDate,
        keyWorkerStaffId = null,
        arrival = null,
        departure = null,
        nonArrival = null,
        cancellation = null,
        extensions = mutableListOf(),
        premises = premises
      )
    )

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(booking, offenderResult.entity, inmateDetailResult.entity, null))
  }

  override fun premisesPremisesIdBookingsBookingIdArrivalsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewArrival
  ): ResponseEntity<Arrival> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val result = bookingService.createArrival(
      booking = booking,
      arrivalDate = body.arrivalDate,
      expectedDepartureDate = body.expectedDepartureDate,
      notes = body.notes,
      keyWorkerStaffId = body.keyWorkerStaffId
    )

    val departure = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(arrivalTransformer.transformJpaToApi(departure))
  }

  override fun premisesPremisesIdBookingsBookingIdNonArrivalsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewNonarrival
  ): ResponseEntity<Nonarrival> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val result = bookingService.createNonArrival(
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

    val result = bookingService.createCancellation(
      booking = booking,
      date = body.date,
      reasonId = body.reason,
      notes = body.notes
    )

    val cancellation = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(cancellationTransformer.transformJpaToApi(cancellation))
  }

  override fun premisesPremisesIdBookingsBookingIdDeparturesPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewDeparture
  ): ResponseEntity<Departure> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val result = bookingService.createDeparture(
      booking = booking,
      dateTime = body.dateTime,
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

    val result = premisesService.createLostBeds(
      premises = premises,
      startDate = body.startDate,
      endDate = body.endDate,
      numberOfBeds = body.numberOfBeds,
      reasonId = body.reason,
      referenceNumber = body.referenceNumber,
      notes = body.notes
    )

    val lostBeds = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(lostBeds))
  }

  override fun premisesPremisesIdLostBedsGet(premisesId: UUID): ResponseEntity<List<LostBed>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    return ResponseEntity.ok(premises.lostBeds.map(lostBedsTransformer::transformJpaToApi))
  }

  override fun premisesPremisesIdCapacityGet(premisesId: UUID): ResponseEntity<List<DateCapacity>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

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

    if (premises !is ApprovedPremisesEntity) {
      throw NotImplementedProblem("Fetching staff for non-AP Premises is not currently supported")
    }

    val staffMembersResult = staffMemberService.getStaffMembersForDeliusTeam(premises.deliusTeamCode)

    val staffMembers = when (staffMembersResult) {
      is AuthorisableActionResult.Success -> staffMembersResult.entity
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw InternalServerErrorProblem("No team found for Delius team code: ${premises.deliusTeamCode}")
    }

    return ResponseEntity.ok(staffMembers.map(staffMemberTransformer::transformDomainToApi))
  }

  override fun premisesPremisesIdRoomsGet(premisesId: UUID): ResponseEntity<List<Room>> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    return ResponseEntity.ok(premises.rooms.map(roomTransformer::transformJpaToApi))
  }

  override fun premisesPremisesIdRoomsPost(premisesId: UUID, newRoom: NewRoom): ResponseEntity<Room> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    val room = extractResultEntityOrThrow(
      roomService.createRoom(premises, newRoom.name, newRoom.notes, newRoom.characteristicIds)
    )

    return ResponseEntity(roomTransformer.transformJpaToApi(room), HttpStatus.CREATED)
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
  }
}
