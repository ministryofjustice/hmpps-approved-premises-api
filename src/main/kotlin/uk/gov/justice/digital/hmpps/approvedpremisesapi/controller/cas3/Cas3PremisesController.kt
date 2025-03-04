package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.PremisesCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class Cas3PremisesController(
  private val userService: UserService,
  private val cas3BookingService: Cas3BookingService,
  private val cas3PremisesService: Cas3PremisesService,
  private val cas3FutureBookingTransformer: Cas3FutureBookingTransformer,
  private val cas3PremisesSummaryTransformer: Cas3PremisesSummaryTransformer,
) : PremisesCas3Delegate {
  override fun getPremisesSummary(): ResponseEntity<List<Cas3PremisesSummary>> {
    val user = userService.getUserForRequest()
    val summaries = cas3PremisesService.getAllPremisesSummaries(user.probationRegion.id)
    val transformedSummaries = summaries.map(cas3PremisesSummaryTransformer::transformDomainToCas3PremisesSummary)

    return ResponseEntity.ok(transformedSummaries)
  }

  override fun getPremisesFutureBookings(premisesId: UUID, statuses: List<BookingStatus>): ResponseEntity<List<FutureBooking>> {
    val user = userService.getUserForRequest()

    val result = cas3BookingService.findFutureBookingsForPremises(premisesId, statuses, user)

    val futureBookings = extractEntityFromCasResult(result)

    return ResponseEntity.ok(
      futureBookings.map { fb -> cas3FutureBookingTransformer.transformJpaToApi(fb.booking, fb.personInfo) },
    )
  }
}
