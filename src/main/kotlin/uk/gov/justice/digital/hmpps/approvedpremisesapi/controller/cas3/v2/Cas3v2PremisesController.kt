package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3.v2

import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.v2.PremisesCas3v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BookingTransformer
import java.util.UUID

@Service
class Cas3v2PremisesController(
  private val usersService: UserService,
  private val userAccessService: UserAccessService,
  private val cas3PremisesService: Cas3v2PremisesService,
  private val cas3BookingService: Cas3v2BookingService,
  private val offenderService: OffenderService,
  private val bookingTransformer: Cas3BookingTransformer,
  private val requestContextService: RequestContextService,
) : PremisesCas3v2Delegate {

  override fun premisesPremisesIdBookingsGet(premisesId: UUID): ResponseEntity<List<Cas3Booking>> = runBlocking {
    requestContextService.ensureCas3Request()

    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val user = usersService.getUserForRequest()
    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val personInfoResults = async {
      offenderService.getPersonInfoResults(
        crns = premises.bookings.map { it.crn }.toSet(),
        laoStrategy = user.cas3LaoStrategy(),
      )
    }.await()

    return@runBlocking ResponseEntity.ok(
      premises.bookings.map { booking ->
        bookingTransformer.transformJpaToApi(
          jpa = booking,
          personInfo = personInfoResults.firstOrNull { pi -> pi.crn == booking.crn } ?: PersonInfoResult.Unknown(booking.crn),
        )
      },
    )
  }

  @Transactional
  override fun premisesPremisesIdBookingsPost(premisesId: UUID, body: NewBooking): ResponseEntity<Cas3Booking> {
    requestContextService.ensureCas3Request()

    // retrieve user so that wer have everything we need
    val user = usersService.getUserForRequest()
    val crn = body.crn.uppercase()

    val premises = cas3PremisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.userCanManageCas3PremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val personInfo =
      offenderService.getPersonInfoResult(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    if (personInfo !is PersonInfoResult.Success) throw InternalServerErrorProblem("Unable to get Person Info for CRN: $crn")

    val createdBookingResult = cas3BookingService.createBooking(
      user = user,
      premises = premises,
      crn = crn,
      nomsNumber = when (personInfo) {
        is PersonInfoResult.Success.Restricted -> personInfo.nomsNumber
        is PersonInfoResult.Success.Full -> personInfo.inmateDetail?.offenderNo
      },
      arrivalDate = body.arrivalDate,
      departureDate = body.departureDate,
      bedspaceId = body.bedId,
      assessmentId = body.assessmentId,
      enableTurnarounds = body.enableTurnarounds ?: false,
    )

    val validatableResult = when (createdBookingResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is AuthorisableActionResult.Success -> createdBookingResult.entity
    }

    val createdBooking = when (validatableResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validatableResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validatableResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = validatableResult.conflictingEntityId,
        conflictReason = validatableResult.message,
      )

      is ValidatableActionResult.Success -> validatableResult.entity
    }

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(createdBooking, personInfo))
  }
}
