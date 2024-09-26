package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.BookingsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.util.UUID

@Service
class BookingsController(
  private val bookingSearchService: BookingSearchService,
  private val bookingSearchResultTransformer: BookingSearchResultTransformer,
  private val bookingService: BookingService,
  private val bookingTransformer: BookingTransformer,
) : BookingsApiDelegate {

  override fun bookingsBookingIdGet(bookingId: UUID): ResponseEntity<Booking> {
    val bookingAndPersons = when (val result = bookingService.getBooking(bookingId)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(bookingId, result.entityType!!)
      is AuthorisableActionResult.Success -> result.entity
    }

    val apiBooking = bookingTransformer.transformJpaToApi(
      bookingAndPersons.booking,
      bookingAndPersons.personInfo,
      bookingAndPersons.staffMember,
    )

    return ResponseEntity.ok(apiBooking)
  }

  override fun bookingsSearchGet(
    status: BookingStatus?,
    sortOrder: SortOrder?,
    sortField: BookingSearchSortField?,
    page: Int?,
    crnOrName: String?,
  ): ResponseEntity<BookingSearchResults> {
    val sortOrder = sortOrder ?: SortOrder.ascending
    val sortField = sortField ?: BookingSearchSortField.bookingCreatedAt

    val (results, metadata) = bookingSearchService.findBookings(status, sortOrder, sortField, page, crnOrName)

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(
        bookingSearchResultTransformer.transformDomainToApi(results),
      )
  }
}
