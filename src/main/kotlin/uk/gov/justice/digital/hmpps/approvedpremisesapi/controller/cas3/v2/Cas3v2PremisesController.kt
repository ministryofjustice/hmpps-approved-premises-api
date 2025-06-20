package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3.v2

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.v2.PremisesCas3v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BookingTransformer
import java.util.UUID

@Service
class Cas3v2PremisesController(
  private val usersService: UserService,
  private val userAccessService: UserAccessService,
  private val cas3PremisesService: Cas3v2PremisesService,
  private val offenderService: OffenderService,
  private val bookingTransformer: Cas3BookingTransformer,
) : PremisesCas3v2Delegate {

  override fun premisesPremisesIdBookingsGet(premisesId: UUID): ResponseEntity<List<Cas3Booking>> = runBlocking {
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
}
