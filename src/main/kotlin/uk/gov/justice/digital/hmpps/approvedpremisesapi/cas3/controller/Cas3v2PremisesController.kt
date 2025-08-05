package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.NewCas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3v2VoidBedspaceService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspacesTransformer
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@SuppressWarnings("LongParameterList")
@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2PremisesController(
  private val usersService: UserService,
  private val userAccessService: UserAccessService,
  private val cas3PremisesService: Cas3v2PremisesService,
  private val cas3BookingService: Cas3v2BookingService,
  private val bookingTransformer: Cas3BookingTransformer,
  private val voidBedspaceService: Cas3v2VoidBedspaceService,
  private val cas3CancellationTransformer: Cas3CancellationTransformer,
  private val offenderDetailService: OffenderDetailService,
  private val cas3UserAccessService: Cas3UserAccessService,
  private val cas3VoidBedspacesTransformer: Cas3VoidBedspacesTransformer,
  private val cas3ArrivalTransformer: Cas3ArrivalTransformer,
) {

  @GetMapping("/premises/{premisesId}/bookings")
  fun getPremises(@PathVariable premisesId: UUID): ResponseEntity<List<Cas3Booking>> = runBlocking {
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
  fun premisesPremisesIdBookingsBookingIdGet(@PathVariable premisesId: UUID, @PathVariable bookingId: UUID): ResponseEntity<Cas3Booking> = runBlocking {
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
  fun premisesPremisesIdBookingsPost(
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
    )

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(extractEntityFromCasResult(createdBookingResult), personInfo))
  }

  @GetMapping("/premises/{premisesId}/void-bedspaces")
  fun getVoidBedspaces(@PathVariable premisesId: UUID): ResponseEntity<List<Cas3VoidBedspace>> {
    val premises = cas3PremisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    val probationRegionId = premises.probationDeliveryUnit.probationRegion.id

    if (!cas3UserAccessService.canViewVoidBedspaces(probationRegionId)) throw ForbiddenProblem()

    val voidBedspaces = voidBedspaceService.findVoidBedspaces(premises.id)
      .map(cas3VoidBedspacesTransformer::toCas3VoidBedspace)

    return ResponseEntity.ok(voidBedspaces)
  }

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/arrivals")
  fun premisesPremisesIdBookingsBookingIdArrivalsPost(
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

    return ResponseEntity.ok(cas3ArrivalTransformer.transformJpaToApi(extractEntityFromCasResult(result)))
  }

  @PostMapping("/premises/{premisesId}/bookings/{bookingId}/cancellations")
  fun premisesPremisesIdBookingsBookingIdCancellationsPost(
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
    val cancellation = extractEntityFromCasResult(result)
    return ResponseEntity.ok(cas3CancellationTransformer.transformJpaToApi(cancellation))
  }

  private fun getBookingForPremisesOrThrow(premisesId: UUID, bookingId: UUID, user: UserEntity): Cas3BookingEntity {
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")
    val bookingResult = cas3BookingService.getBooking(bookingId, premises.id, user)
    return extractEntityFromCasResult(bookingResult)
  }
}
