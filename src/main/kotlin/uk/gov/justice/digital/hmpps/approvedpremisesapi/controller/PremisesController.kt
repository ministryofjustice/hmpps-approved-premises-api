package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import arrow.core.Ior
import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTurnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.NewCas2Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotImplementedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestContextService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3VoidBedspaceService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DateChangeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExtensionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoomTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3ConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3TurnaroundTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3VoidBedspaceCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3VoidBedspacesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromAuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.GetBookingForPremisesResult as cas3GetBookingForPremisesResult

@Service
class PremisesController(
  private val usersService: UserService,
  private val userAccessService: UserAccessService,
  private val premisesService: PremisesService,
  private val cas3PremisesService: Cas3PremisesService,
  private val bookingService: BookingService,
  private val cas3BookingService: Cas3BookingService,
  private val cas3VoidBedspaceService: Cas3VoidBedspaceService,
  private val premisesTransformer: PremisesTransformer,
  private val bookingTransformer: BookingTransformer,
  private val cas3VoidBedspacesTransformer: Cas3VoidBedspacesTransformer,
  private val arrivalTransformer: ArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val cas3ConfirmationTransformer: Cas3ConfirmationTransformer,
  private val departureTransformer: DepartureTransformer,
  private val extensionTransformer: ExtensionTransformer,
  private val roomService: RoomService,
  private val roomTransformer: RoomTransformer,
  private val cas3VoidBedspaceCancellationTransformer: Cas3VoidBedspaceCancellationTransformer,
  private val cas3TurnaroundTransformer: Cas3TurnaroundTransformer,
  private val bedSummaryTransformer: BedSummaryTransformer,
  private val dateChangeTransformer: DateChangeTransformer,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val requestContextService: RequestContextService,
  private val offenderDetailService: OffenderDetailService,
) : PremisesApiDelegate {
  @Transactional
  override fun premisesPremisesIdPut(premisesId: UUID, body: UpdatePremises): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val serviceName = when (premises) {
      is TemporaryAccommodationPremisesEntity -> ServiceName.temporaryAccommodation
      else -> ServiceName.approvedPremises
    }

    if (!userAccessService.currentUserCanManagePremises(premises) || !userAccessService.currentUserCanAccessRegion(serviceName, body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val updatePremisesResult = when (serviceName) {
      ServiceName.temporaryAccommodation ->
        cas3PremisesService
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

      else ->
        premisesService
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
          )
    }

    var validationResult = when (updatePremisesResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(premisesId, "Premises")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> updatePremisesResult.entity
    }

    val bodyName = body.name
    if (bodyName != null && serviceName == ServiceName.temporaryAccommodation) {
      validationResult = when (val renamePremisesResult = cas3PremisesService.renamePremises(premisesId, bodyName)) {
        is AuthorisableActionResult.NotFound -> throw NotFoundProblem(premisesId, "Premises")
        is AuthorisableActionResult.Success -> renamePremisesResult.entity
        is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      }
    }

    val updatedPremises = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages.mapValues { ParamDetails(it.value) })
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = validationResult.conflictingEntityId,
        conflictReason = validationResult.message,
      )

      is ValidatableActionResult.Success -> validationResult.entity
    }

    val totalBeds = when (serviceName) {
      ServiceName.temporaryAccommodation -> cas3PremisesService.getBedspaceCount(updatedPremises)
      else -> premisesService.getBedCount(updatedPremises)
    }

    return ResponseEntity.ok(
      premisesTransformer.transformJpaToApi(
        updatedPremises,
        totalBeds = totalBeds,
        availableBedsForToday = totalBeds,
      ),
    )
  }

  override fun premisesPost(body: NewPremises, xServiceName: ServiceName?): ResponseEntity<Premises> {
    val serviceName = when (xServiceName == null) {
      true -> ServiceName.approvedPremises
      false -> xServiceName
    }

    if (!userAccessService.currentUserCanAccessRegion(serviceName, body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val premises: PremisesEntity
    val totalBeds: Int

    when (xServiceName) {
      ServiceName.temporaryAccommodation -> {
        premises = extractEntityFromCasResult(
          cas3PremisesService.createNewPremises(
            addressLine1 = body.addressLine1,
            addressLine2 = body.addressLine2,
            town = body.town,
            postcode = body.postcode,
            localAuthorityAreaId = body.localAuthorityAreaId,
            probationRegionId = body.probationRegionId,
            name = body.name,
            notes = body.notes,
            characteristicIds = body.characteristicIds,
            status = body.status,
            probationDeliveryUnitIdentifier = Ior.fromNullables(body.pdu, body.probationDeliveryUnitId)?.toEither(),
            turnaroundWorkingDays = body.turnaroundWorkingDayCount,
          ),
        )

        totalBeds = cas3PremisesService.getBedspaceCount(premises)
      }
      else -> {
        premises = extractResultEntityOrThrow(
          premisesService.createNewPremises(
            addressLine1 = body.addressLine1,
            addressLine2 = body.addressLine2,
            town = body.town,
            postcode = body.postcode,
            latitude = null,
            longitude = null,
            service = serviceName.value,
            localAuthorityAreaId = body.localAuthorityAreaId,
            probationRegionId = body.probationRegionId,
            name = body.name,
            notes = body.notes,
            characteristicIds = body.characteristicIds,
            status = body.status,
            probationDeliveryUnitIdentifier = Ior.fromNullables(body.pdu, body.probationDeliveryUnitId)?.toEither(),
            turnaroundWorkingDays = body.turnaroundWorkingDayCount,
          ),
        )

        totalBeds = premisesService.getBedCount(premises)
      }
    }

    return ResponseEntity(
      premisesTransformer.transformJpaToApi(
        premises,
        totalBeds = totalBeds,
        availableBedsForToday = totalBeds,
      ),
      HttpStatus.CREATED,
    )
  }

  override fun premisesPremisesIdGet(premisesId: UUID): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val totalBeds: Int
    val availableBedsForToday: Int

    when (premises) {
      is TemporaryAccommodationPremisesEntity -> {
        totalBeds = cas3PremisesService.getBedspaceCount(premises)
        availableBedsForToday =
          cas3PremisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
            .values.first().getFreeCapacity(totalBeds)
      }
      else -> {
        totalBeds = premisesService.getBedCount(premises)
        availableBedsForToday =
          premisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
            .values.first().getFreeCapacity(totalBeds)
      }
    }

    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(premises, totalBeds, availableBedsForToday))
  }

  override fun premisesPremisesIdBookingsGet(premisesId: UUID): ResponseEntity<List<Booking>> = runBlocking {
    requestContextService.ensureCas3Request()

    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManageCas3PremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val crns = premises.bookings.map { it.crn }
    val personInfoResults = async {
      offenderDetailService.getPersonInfoResults(
        crns.toSet(),
        user.cas3LaoStrategy(),
      )
    }.await()

    return@runBlocking ResponseEntity.ok(
      premises.bookings.map {
        val crn = it.crn
        val personInfo = personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn)
        bookingTransformer.transformJpaToApi(it, personInfo)
      },
    )
  }

  override fun premisesPremisesIdBookingsBookingIdGet(premisesId: UUID, bookingId: UUID): ResponseEntity<Booking> {
    val bookingResult = bookingService.getBooking(bookingId)

    val bookingAndPersons = when (bookingResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(bookingResult.id!!, bookingResult.entityType!!)
      is AuthorisableActionResult.Success -> bookingResult.entity
    }

    val apiBooking = bookingTransformer.transformJpaToApi(
      bookingAndPersons.booking,
      bookingAndPersons.personInfo,
    )

    if (apiBooking.premises.id != premisesId) {
      throw NotFoundProblem(premisesId, "Premises")
    }

    return ResponseEntity.ok(apiBooking)
  }

  @Transactional
  override fun premisesPremisesIdBookingsPost(premisesId: UUID, body: NewBooking): ResponseEntity<Booking> {
    requestContextService.ensureCas3Request()

    val user = usersService.getUserForRequest()
    val crn = body.crn.uppercase()

    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.userCanManageCas3PremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val personInfo =
      offenderDetailService.getPersonInfoResult(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    if (personInfo !is PersonInfoResult.Success) throw InternalServerErrorProblem("Unable to get Person Info for CRN: $crn")

    val createdBooking = when (premises) {
      is TemporaryAccommodationPremisesEntity -> {
        cas3BookingService.createBooking(
          user = user,
          premises = premises,
          crn = crn,
          nomsNumber = when (personInfo) {
            is PersonInfoResult.Success.Restricted -> personInfo.nomsNumber
            is PersonInfoResult.Success.Full -> personInfo.inmateDetail?.offenderNo
          },
          arrivalDate = body.arrivalDate,
          departureDate = body.departureDate,
          bedId = body.bedId,
          assessmentId = body.assessmentId,
          enableTurnarounds = body.enableTurnarounds ?: false,
        )
      }

      else -> error("This endpoint does not support creating bookings for premise type: ${premises::class.qualifiedName}")
    }

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(extractEntityFromCasResult(createdBooking), personInfo))
  }

  override fun premisesPremisesIdBookingsBookingIdArrivalsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewArrival,
  ): ResponseEntity<Arrival> {
    requestContextService.ensureCas3Request()

    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManageCas3PremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = when (body) {
      is NewCas2Arrival -> {
        val bedId = booking.bed?.id
          ?: throw InternalServerErrorProblem("No bed ID present on Booking: $bookingId")

        throwIfBookingDatesConflict(body.arrivalDate, body.expectedDepartureDate, bookingId, bedId)
        throwIfVoidBedspaceDatesConflict(body.arrivalDate, body.expectedDepartureDate, null, bedId)

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
        throwIfVoidBedspaceDatesConflict(body.arrivalDate, body.expectedDepartureDate, null, bedId)

        cas3BookingService.createArrival(
          booking = booking,
          arrivalDate = body.arrivalDate,
          expectedDepartureDate = body.expectedDepartureDate,
          notes = body.notes,
          user = user,
        )
      }

      else -> throw RuntimeException("Unsupported NewArrival type: ${body::class.qualifiedName}")
    }

    val arrival = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(arrivalTransformer.transformJpaToApi(arrival))
  }

  override fun premisesPremisesIdBookingsBookingIdCancellationsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewCancellation,
  ): ResponseEntity<Cancellation> {
    val user = usersService.getUserForRequest()

    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.userMayCancelBooking(user, booking)) {
      throw ForbiddenProblem()
    }

    when (booking.premises) {
      is ApprovedPremisesEntity -> {
        val result = cas1WithdrawableService.withdrawBooking(
          booking = booking,
          user = user,
          cancelledAt = body.date,
          userProvidedReason = body.reason,
          notes = body.notes,
          otherReason = body.otherReason,
        )
        val cancellation = extractEntityFromCasResult(result)
        return ResponseEntity.ok(cancellationTransformer.transformJpaToApi(cancellation))
      }

      is TemporaryAccommodationPremisesEntity -> {
        val result = cas3BookingService.createCancellation(
          booking = booking,
          cancelledAt = body.date,
          reasonId = body.reason,
          notes = body.notes,
          user = user,
        )
        val cancellation = extractResultEntityOrThrow(result)
        return ResponseEntity.ok(cancellationTransformer.transformJpaToApi(cancellation))
      }

      else -> throw NotImplementedProblem("Unsupported premises type ${booking.premises::class.qualifiedName}")
    }
  }

  override fun premisesPremisesIdBookingsBookingIdConfirmationsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewConfirmation,
  ): ResponseEntity<Confirmation> {
    requestContextService.ensureCas3Request()

    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.userCanManageCas3PremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result =
      when (booking.premises) {
        is TemporaryAccommodationPremisesEntity -> {
          cas3BookingService.createConfirmation(
            user = user,
            booking = booking,
            dateTime = OffsetDateTime.now(),
            notes = body.notes,
          )
        }
        else -> {
          bookingService.createConfirmation(
            booking = booking,
            dateTime = OffsetDateTime.now(),
            notes = body.notes,
          )
        }
      }
    val confirmation = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(cas3ConfirmationTransformer.transformJpaToApi(confirmation))
  }

  override fun premisesPremisesIdBookingsBookingIdDeparturesPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewDeparture,
  ): ResponseEntity<Departure> {
    requestContextService.ensureCas3Request()

    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManageCas3PremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    when (booking.premises) {
      is TemporaryAccommodationPremisesEntity -> {
        val result = cas3BookingService.createDeparture(
          user = user,
          booking = booking,
          dateTime = body.dateTime.atOffset(ZoneOffset.UTC),
          reasonId = body.reasonId,
          moveOnCategoryId = body.moveOnCategoryId,
          notes = body.notes,
        )

        val departure = extractEntityFromCasResult(result)

        return ResponseEntity.ok(departureTransformer.transformJpaToApi(departure))
      }

      else -> error("This endpoint does not support recording departures for bookings with premise type: ${booking.premises::class.qualifiedName}")
    }
  }

  override fun premisesPremisesIdBookingsBookingIdExtensionsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewExtension,
  ): ResponseEntity<Extension> {
    requestContextService.ensureCas3Request()

    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    when (booking.premises) {
      is TemporaryAccommodationPremisesEntity -> {
        if (!userAccessService.currentUserCanManageCas3PremisesBookings(booking.premises)) {
          throw ForbiddenProblem()
        }

        val result = cas3BookingService.createExtension(
          booking = booking,
          newDepartureDate = body.newDepartureDate,
          notes = body.notes,
        )

        val extension = extractResultEntityOrThrow(result)

        return ResponseEntity.ok(extensionTransformer.transformJpaToApi(extension))
      }

      else -> error("This endpoint does not support create booking extension with premise type: ${booking.premises::class.qualifiedName}")
    }
  }

  @Transactional
  override fun premisesPremisesIdBookingsBookingIdDateChangesPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewDateChange,
  ): ResponseEntity<DateChange> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)
    val user = usersService.getUserForRequest()

    if (!userAccessService.currentUserCanChangeBookingDate(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createDateChange(
      booking = booking,
      user = user,
      newArrivalDate = body.newArrivalDate,
      newDepartureDate = body.newDepartureDate,
    )

    val dateChange = extractEntityFromCasResult(result)

    return ResponseEntity.ok(dateChangeTransformer.transformJpaToApi(dateChange))
  }

  override fun premisesPremisesIdLostBedsPost(premisesId: UUID, body: NewLostBed): ResponseEntity<LostBed> {
    throwIfRequestIsForApprovedPremises("POST /cas1/premises/$premisesId/lost-beds")

    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremisesVoidBedspaces(premises)) {
      throw ForbiddenProblem()
    }

    throwIfBookingDatesConflict(body.startDate, body.endDate, null, body.bedId)
    throwIfVoidBedspaceDatesConflict(body.startDate, body.endDate, null, body.bedId)

    val result = cas3PremisesService.createVoidBedspaces(
      premises = premises,
      startDate = body.startDate,
      endDate = body.endDate,
      reasonId = body.reason,
      referenceNumber = body.referenceNumber,
      notes = body.notes,
      bedId = body.bedId,
    )

    val voidBedspaces = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(cas3VoidBedspacesTransformer.transformJpaToApi(voidBedspaces))
  }

  override fun premisesPremisesIdLostBedsGet(premisesId: UUID): ResponseEntity<List<LostBed>> {
    throwIfRequestIsForApprovedPremises("GET /cas1/premises/$premisesId/lost-beds")

    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val voidBedspaces = cas3VoidBedspaceService.getActiveVoidBedspacesForPremisesId(premisesId)

    if (!userAccessService.currentUserCanManagePremisesVoidBedspaces(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(voidBedspaces.map(cas3VoidBedspacesTransformer::transformJpaToApi))
  }

  override fun premisesPremisesIdLostBedsLostBedIdGet(premisesId: UUID, lostBedId: UUID): ResponseEntity<LostBed> {
    throwIfRequestIsForApprovedPremises("GET /cas1/premises/$premisesId/lost-beds/$lostBedId")

    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremisesVoidBedspaces(premises)) {
      throw ForbiddenProblem()
    }

    val voidBedspace = premises.voidBedspaces.firstOrNull { it.id == lostBedId }
      ?: throw NotFoundProblem(lostBedId, "LostBed")

    return ResponseEntity.ok(cas3VoidBedspacesTransformer.transformJpaToApi(voidBedspace))
  }

  override fun premisesPremisesIdLostBedsLostBedIdPut(
    premisesId: UUID,
    lostBedId: UUID,
    body: UpdateLostBed,
  ): ResponseEntity<LostBed> {
    throwIfRequestIsForApprovedPremises("PUT /cas1/premises/$premisesId/lost-beds/$lostBedId")

    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    val voidBedspace = premises.voidBedspaces.firstOrNull { it.id == lostBedId } ?: throw NotFoundProblem(lostBedId, "VoidBedspace")

    if (!userAccessService.currentUserCanManagePremisesVoidBedspaces(premises)) {
      throw ForbiddenProblem()
    }

    throwIfBookingDatesConflict(body.startDate, body.endDate, null, voidBedspace.bed!!.id)
    throwIfVoidBedspaceDatesConflict(body.startDate, body.endDate, lostBedId, voidBedspace.bed!!.id)

    val updateVoidBedspaceResult = cas3PremisesService
      .updateVoidBedspaces(
        lostBedId,
        body.startDate,
        body.endDate,
        body.reason,
        body.referenceNumber,
        body.notes,
      )

    val validationResult = when (updateVoidBedspaceResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(lostBedId, "VoidBedspace")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> updateVoidBedspaceResult.entity
    }

    val updatedVoidBedspace = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages.mapValues { ParamDetails(it.value) })
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = validationResult.conflictingEntityId,
        conflictReason = validationResult.message,
      )

      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(cas3VoidBedspacesTransformer.transformJpaToApi(updatedVoidBedspace))
  }

  override fun premisesPremisesIdLostBedsLostBedIdCancellationsPost(
    premisesId: UUID,
    lostBedId: UUID,
    body: NewLostBedCancellation,
  ): ResponseEntity<LostBedCancellation> {
    throwIfRequestIsForApprovedPremises("POST /cas1/premises/$premisesId/lost-beds/$lostBedId/cancellations")

    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    val voidBedspace = premises.voidBedspaces.firstOrNull { it.id == lostBedId } ?: throw NotFoundProblem(lostBedId, "VoidBedspace")

    if (!userAccessService.currentUserCanManagePremisesVoidBedspaces(premises)) {
      throw ForbiddenProblem()
    }

    val cancelVoidBedspaceResult = cas3PremisesService.cancelVoidBedspace(
      voidBedspace = voidBedspace,
      notes = body.notes,
    )

    val cancellation = when (cancelVoidBedspaceResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = cancelVoidBedspaceResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = cancelVoidBedspaceResult.validationMessages.mapValues { ParamDetails(it.value) })
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = cancelVoidBedspaceResult.conflictingEntityId,
        conflictReason = cancelVoidBedspaceResult.message,
      )

      is ValidatableActionResult.Success -> cancelVoidBedspaceResult.entity
    }

    return ResponseEntity.ok(cas3VoidBedspaceCancellationTransformer.transformJpaToApi(cancellation))
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
      roomService.createRoom(premises, newRoom.name, newRoom.notes, newRoom.characteristicIds, newRoom.bedEndDate),
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

  @Transactional
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

    var validationResult = when (updateRoomResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(roomId, "Room")
      is AuthorisableActionResult.Success -> updateRoomResult.entity
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    if (premises is TemporaryAccommodationPremisesEntity) {
      val updateRoomName = updateRoom.name
      if (updateRoomName != null) {
        validationResult =
          extractEntityFromAuthorisableActionResult(roomService.renameRoom(premises, roomId, updateRoomName))
      }
      val updateRoomEndDate = updateRoom.bedEndDate
      if (updateRoomEndDate != null) {
        validationResult = extractEntityFromAuthorisableActionResult(
          roomService.updateBedEndDate(
            premises,
            roomId,
            updateRoomEndDate,
          ),
        )
      }
    }

    val room = extractResultEntityOrThrow(validationResult)

    return ResponseEntity.ok(roomTransformer.transformJpaToApi(room))
  }

  override fun premisesPremisesIdBookingsBookingIdTurnaroundsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewTurnaround,
  ): ResponseEntity<Turnaround> {
    requestContextService.ensureCas3Request()

    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    when (booking.premises) {
      is TemporaryAccommodationPremisesEntity -> {
        if (!userAccessService.currentUserCanManageCas3PremisesBookings(booking.premises)) {
          throw ForbiddenProblem()
        }

        val result = cas3BookingService.createTurnaround(booking, body.workingDays)
        val turnaround = extractResultEntityOrThrow(result)

        return ResponseEntity.ok(cas3TurnaroundTransformer.transformJpaToApi(turnaround))
      }

      else -> error("This endpoint does not support create turnarounds for bookings with premise type: ${booking.premises::class.qualifiedName}")
    }
  }

  override fun premisesPremisesIdBedsGet(premisesId: UUID): ResponseEntity<List<BedSummary>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(premisesService.getBeds(premisesId).map(bedSummaryTransformer::transformToApi))
  }

  @SuppressWarnings("ThrowsCount")
  private fun getBookingForPremisesOrThrow(premisesId: UUID, bookingId: UUID): BookingEntity {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    return when (premises) {
      is TemporaryAccommodationPremisesEntity -> {
        when (val result = cas3BookingService.getBookingForPremises(premises, bookingId)) {
          is cas3GetBookingForPremisesResult.Success -> result.booking
          is cas3GetBookingForPremisesResult.BookingNotFound -> throw NotFoundProblem(bookingId, "Booking")
        }
      }
      else -> {
        when (val result = bookingService.getBookingForPremises(premises, bookingId)) {
          is GetBookingForPremisesResult.Success -> result.booking
          is GetBookingForPremisesResult.BookingNotFound -> throw NotFoundProblem(bookingId, "Booking")
        }
      }
    }
  }

  private fun <EntityType> extractResultEntityOrThrow(result: ValidatableActionResult<EntityType>) = when (result) {
    is ValidatableActionResult.Success -> result.entity
    is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = result.message)
    is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = result.validationMessages.mapValues { ParamDetails(it.value) })
    is ValidatableActionResult.ConflictError -> throw ConflictProblem(
      id = result.conflictingEntityId,
      conflictReason = result.message,
    )
  }

  private fun throwIfBookingDatesConflict(
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) {
    bookingService.getBookingWithConflictingDates(arrivalDate, departureDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(
        it.id,
        "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates",
      )
    }
  }

  private fun throwIfVoidBedspaceDatesConflict(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) {
    bookingService.getVoidBedspaceWithConflictingDates(startDate, endDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(
        it.id,
        "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates",
      )
    }
  }

  private fun throwIfRequestIsForApprovedPremises(endpoint: String) {
    getRequest().ifPresent {
      if (it.getHeader("X-Service-Name") == ServiceName.approvedPremises.value) {
        throw BadRequestProblem(errorDetail = "CAS1 not supported. Use `$endpoint` instead")
      }
    }
  }
}
