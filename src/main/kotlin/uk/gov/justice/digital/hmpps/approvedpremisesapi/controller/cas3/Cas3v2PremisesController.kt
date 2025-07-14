package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3v2VoidBedspaceService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3VoidBedspacesTransformer
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
  private val offenderDetailService: OffenderDetailService,
  private val cas3UserAccessService: Cas3UserAccessService,
  private val cas3VoidBedspacesTransformer: Cas3VoidBedspacesTransformer,
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
    val bookingAndPersonsResult = cas3BookingService.getBooking(bookingId, premisesId)
    val bookingAndPersons = extractEntityFromCasResult(bookingAndPersonsResult)
    val apiBooking = bookingTransformer.transformJpaToApi(
      bookingAndPersons.booking,
      bookingAndPersons.personInfo,
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
}
