package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Nonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.KeyWorkerService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExtensionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import java.time.LocalDate
import java.util.UUID

@Service
class PremisesController(
  private val premisesService: PremisesService,
  private val offenderService: OffenderService,
  private val keyWorkerService: KeyWorkerService,
  private val bookingService: BookingService,
  private val premisesTransformer: PremisesTransformer,
  private val bookingTransformer: BookingTransformer,
  private val lostBedsTransformer: LostBedsTransformer,
  private val arrivalTransformer: ArrivalTransformer,
  private val nonArrivalTransformer: NonArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val departureTransformer: DepartureTransformer,
  private val extensionTransformer: ExtensionTransformer
) : PremisesApiDelegate {
  override fun premisesGet(): ResponseEntity<List<Premises>> {
    return ResponseEntity.ok(
      premisesService.getAllPremises().map {
        val availableBedsForToday = premisesService.getAvailabilityForRange(it, LocalDate.now(), LocalDate.now().plusDays(1))
          .values.first().getFreeCapacity(it.totalBeds)

        premisesTransformer.transformJpaToApi(it, availableBedsForToday)
      }
    )
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

    return ResponseEntity.ok(
      premises.bookings.map {
        val offenderResult = offenderService.getOffenderByCrn(it.crn, getDeliusPrincipalNameOrThrow())

        if (offenderResult !is AuthorisableActionResult.Success) {
          throw InternalServerErrorProblem("Unable to get Person via crn: ${it.crn}")
        }

        bookingTransformer.transformJpaToApi(it, offenderResult.entity)
      }
    )
  }

  override fun premisesPremisesIdBookingsBookingIdGet(premisesId: UUID, bookingId: UUID): ResponseEntity<Booking> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val offenderResult = offenderService.getOffenderByCrn(booking.crn, getDeliusPrincipalNameOrThrow())

    if (offenderResult !is AuthorisableActionResult.Success) {
      throw InternalServerErrorProblem("Unable to get Person via crn: ${booking.crn}")
    }

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(booking, offenderResult.entity))
  }

  override fun premisesPremisesIdBookingsPost(premisesId: UUID, body: NewBooking): ResponseEntity<Booking> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val validationErrors = mutableMapOf<String, String>()

    val offenderResult = offenderService.getOffenderByCrn(body.crn, getDeliusPrincipalNameOrThrow())
    if (offenderResult is AuthorisableActionResult.Unauthorised) throw ForbiddenProblem()
    if (offenderResult is AuthorisableActionResult.NotFound) validationErrors["crn"] = "Invalid crn"
    offenderResult as AuthorisableActionResult.Success

    val keyWorker = keyWorkerService.getKeyWorker(body.keyWorkerId)
    if (keyWorker == null) validationErrors["keyWorkerId"] = "Invalid keyWorkerId"

    if (validationErrors.any()) {
      throw BadRequestProblem(validationErrors)
    }

    val booking = bookingService.createBooking(
      BookingEntity(
        id = UUID.randomUUID(),
        crn = offenderResult.entity.otherIds.crn,
        arrivalDate = body.expectedArrivalDate,
        departureDate = body.expectedDepartureDate,
        keyWorker = keyWorker!!,
        arrival = null,
        departure = null,
        nonArrival = null,
        cancellation = null,
        extensions = mutableListOf(),
        premises = premises
      )
    )

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(booking, offenderResult.entity))
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
      notes = body.notes
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
      reason = lostBedsTransformer.transformReasonFromApiToJpa(body.reason),
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

  private fun getDeliusPrincipalNameOrThrow(): String {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (principal.token.claims["auth_source"] != "delius") {
      throw ForbiddenProblem()
    }

    return principal.name
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
