package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTurnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Overstay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.NewOverstay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.NewCas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ExtensionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3OverstayTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3TurnaroundTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SuppressWarnings("LongParameterList")
@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2BookingController(
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val bookingService: Cas3v2BookingService,
  private val bookingSearchService: Cas3v2BookingSearchService,
  private val bookingTransformer: Cas3BookingTransformer,
  private val bookingSearchResultTransformer: Cas3BookingSearchResultTransformer,
  private val usersService: UserService,
  private val userAccessService: UserAccessService,
  private val cas3PremisesService: Cas3v2PremisesService,
  private val cas3BookingService: Cas3v2BookingService,
  private val cas3CancellationTransformer: Cas3CancellationTransformer,
  private val cas3ArrivalTransformer: Cas3ArrivalTransformer,
  private val cas3DepartureTransformer: Cas3DepartureTransformer,
  private val cas3ConfirmationTransformer: Cas3ConfirmationTransformer,
  private val cas3ExtensionTransformer: Cas3ExtensionTransformer,
  private val cas3OverstayTransformer: Cas3OverstayTransformer,
  private val cas3TurnaroundTransformer: Cas3TurnaroundTransformer,
) {

  @GetMapping("/bookings/{bookingId}")
  fun getBookingById(@PathVariable bookingId: UUID): ResponseEntity<Cas3Booking> {
    val user = userService.getUserForRequest()
    val bookingResult = bookingService.getBooking(bookingId, premisesId = null, user)
    val booking = extractEntityFromCasResult(bookingResult)
    val personInfo = offenderDetailService.getPersonInfoResult(
      booking.crn,
      user.deliusUsername,
      user.hasQualification(
        UserQualification.LAO,
      ),
    )

    val apiBooking = bookingTransformer.transformJpaToApi(
      booking,
      personInfo,
    )

    return ResponseEntity.ok(apiBooking)
  }

  @PaginationHeaders
  @GetMapping("/bookings/search")
  fun searchBookings(
    @RequestParam status: Cas3BookingStatus?,
    @RequestParam(defaultValue = "asc") sortDirection: SortDirection,
    @RequestParam(defaultValue = "createdAt") sortField: Cas3BookingSearchSortField,
    @RequestParam page: Int?,
    @RequestParam(required = false) crnOrName: String?,
  ): ResponseEntity<Cas3BookingSearchResults> {
    val (results, metadata) = bookingSearchService.findBookings(
      status,
      sortDirection,
      sortField,
      page,
      crnOrName,
    )
    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(
        bookingSearchResultTransformer.transformDomainToApi(results),
      )
  }

  @GetMapping("/premises/{premisesId}/bookings")
  fun getPremisesBookings(@PathVariable premisesId: UUID): ResponseEntity<List<Cas3Booking>> = runBlocking {
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val user = usersService.getUserForRequest()
    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val personInfoResults = async {
      offenderDetailService.getPersonInfoResults(
        crns = premises.bookings.map { it.crn }.toSet(),
        laoStrategy = user.cas3LaoStrategy(),
      )
    }.await()

    return@runBlocking ResponseEntity.ok(
      premises.bookings.map { booking ->
        bookingTransformer.transformJpaToApi(
          jpa = booking,
          personInfo = personInfoResults.firstOrNull { pi -> pi.crn == booking.crn }
            ?: PersonInfoResult.Unknown(booking.crn),
        )
      },
    )
  }

  @GetMapping("/premises/{premisesId}/bookings/{bookingId}")
  fun getPremisesBookingByBookingId(@PathVariable premisesId: UUID, @PathVariable bookingId: UUID): ResponseEntity<Cas3Booking> = runBlocking {
    val user = usersService.getUserForRequest()
    val bookingResult = cas3BookingService.getBooking(bookingId, premisesId, user)
    val booking = extractEntityFromCasResult(bookingResult)
    val personInfo = offenderDetailService.getPersonInfoResult(
      booking.crn,
      user.deliusUsername,
      user.hasQualification(
        UserQualification.LAO,
      ),
    )
    val apiBooking = bookingTransformer.transformJpaToApi(
      booking,
      personInfo,
    )
    ResponseEntity.ok(apiBooking)
  }

  @PostMapping("/premises/{premisesId}/bookings")
  @Transactional
  @SuppressWarnings("ThrowsCount")
  fun postPremisesBooking(
    @PathVariable premisesId: UUID,
    @RequestBody newBooking: Cas3NewBooking,
  ): ResponseEntity<Cas3Booking> {
    val user = usersService.getUserForRequest()
    val crn = newBooking.crn.uppercase()

    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val personInfo =
      offenderDetailService.getPersonInfoResult(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    if (personInfo !is PersonInfoResult.Success) throw InternalServerErrorProblem("Unable to get Person Info for CRN: $crn")

    val createdBookingResult = cas3BookingService.createBooking(
      user = user,
      premises = premises,
      crn = crn,
      nomsNumber = when (personInfo) {
        is PersonInfoResult.Success.Restricted -> personInfo.nomsNumber
        is PersonInfoResult.Success.Full -> personInfo.inmateDetail?.offenderNo
      },
      arrivalDate = newBooking.arrivalDate,
      departureDate = newBooking.departureDate,
      bedspaceId = newBooking.bedspaceId,
      assessmentId = newBooking.assessmentId,
      enableTurnarounds = newBooking.enableTurnarounds ?: false,
    )
    return ResponseEntity.status(HttpStatus.CREATED).body(
      bookingTransformer.transformJpaToApi(
        jpa = extractEntityFromCasResult(createdBookingResult),
        personInfo,
      ),
    )
  }

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/arrivals")
  fun postPremisesBookingArrival(
    @PathVariable premisesId: UUID,
    @PathVariable bookingId: UUID,
    @RequestBody newArrival: NewCas3Arrival,
  ): ResponseEntity<Cas3Arrival> {
    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId, user)
    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    cas3BookingService.throwIfBookingDatesConflict(newArrival.arrivalDate, newArrival.expectedDepartureDate, bookingId, bedspaceId = booking.bedspace.id)
    cas3BookingService.throwIfVoidBedspaceDatesConflict(newArrival.arrivalDate, newArrival.expectedDepartureDate, bookingId = null, bedspaceId = booking.bedspace.id)

    val result = cas3BookingService.createArrival(
      booking = booking,
      arrivalDate = newArrival.arrivalDate,
      expectedDepartureDate = newArrival.expectedDepartureDate,
      notes = newArrival.notes,
      user = user,
    )

    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3ArrivalTransformer.transformJpaToApi(
        jpa = extractEntityFromCasResult(result),
      ),
    )
  }

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/cancellations")
  fun postPremisesBookingCancellation(
    @PathVariable premisesId: UUID,
    @PathVariable bookingId: UUID,
    @RequestBody body: NewCancellation,
  ): ResponseEntity<Cas3Cancellation> {
    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId, user)
    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = cas3BookingService.createCancellation(
      booking = booking,
      cancelledAt = body.date,
      reasonId = body.reason,
      notes = body.notes,
      user = user,
    )

    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3CancellationTransformer.transformJpaToApi(
        jpa = extractEntityFromCasResult(result),
      ),
    )
  }

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/departures")
  fun postPremisesBookingDeparture(
    @PathVariable premisesId: UUID,
    @PathVariable bookingId: UUID,
    @RequestBody body: Cas3NewDeparture,
  ): ResponseEntity<Cas3Departure> {
    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId, user)
    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = cas3BookingService.createDeparture(
      user = user,
      booking = booking,
      dateTime = body.dateTime.atOffset(ZoneOffset.UTC),
      reasonId = body.reasonId,
      moveOnCategoryId = body.moveOnCategoryId,
      notes = body.notes,
    )

    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3DepartureTransformer.transformJpaToApi(
        jpa = extractEntityFromCasResult(result),
      ),
    )
  }

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/confirmations")
  fun postPremisesBookingDepartureConfirmation(
    @PathVariable premisesId: UUID,
    @PathVariable bookingId: UUID,
    @RequestBody newConfirmation: NewConfirmation,
  ): ResponseEntity<Cas3Confirmation> {
    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId, user)
    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = cas3BookingService.createConfirmation(
      user = user,
      booking = booking,
      dateTime = OffsetDateTime.now(),
      notes = newConfirmation.notes,
    )

    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3ConfirmationTransformer.transformJpaToApi(
        jpa = extractEntityFromCasResult(result),
      ),
    )
  }

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/extensions")
  fun postPremisesBookingExtension(
    @PathVariable premisesId: UUID,
    @PathVariable bookingId: UUID,
    @RequestBody newExtension: NewExtension,
  ): ResponseEntity<Cas3Extension> {
    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId, user)
    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }
    val result = cas3BookingService.createExtension(
      booking = booking,
      newDepartureDate = newExtension.newDepartureDate,
      notes = newExtension.notes,
    )
    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3ExtensionTransformer.transformJpaToApi(
        jpa = extractEntityFromCasResult(result),
      ),
    )
  }

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/overstays")
  fun postPremisesBookingOverstay(
    @PathVariable premisesId: UUID,
    @PathVariable bookingId: UUID,
    @RequestBody newOverstay: NewOverstay,
  ): ResponseEntity<Cas3Overstay> {
    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId, user)

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = cas3BookingService.createOverstay(
      booking = booking,
      newDepartureDate = newOverstay.newDepartureDate,
      isAuthorised = newOverstay.isAuthorised,
      reason = newOverstay.reason,
    )

    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3OverstayTransformer.transformJpaToApi(extractEntityFromCasResult(result)),
    )
  }

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/turnarounds")
  fun postPremisesBookingTurnaround(
    @PathVariable premisesId: UUID,
    @PathVariable bookingId: UUID,
    @RequestBody newTurnaround: NewTurnaround,
  ): ResponseEntity<Cas3Turnaround> {
    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId, user)
    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }
    val result = cas3BookingService.createTurnaround(booking, newTurnaround.workingDays)
    return ResponseEntity.status(HttpStatus.CREATED).body(
      cas3TurnaroundTransformer.transformJpaToApi(
        jpa = extractEntityFromCasResult(result),
      ),
    )
  }

  private fun getBookingForPremisesOrThrow(premisesId: UUID, bookingId: UUID, user: UserEntity): Cas3BookingEntity {
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")
    val bookingResult = cas3BookingService.getBooking(bookingId, premises.id, user)
    return extractEntityFromCasResult(bookingResult)
  }
}
