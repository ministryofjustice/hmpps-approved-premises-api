package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.BookingsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingSearchResultTransformer

@Service
class BookingSearchController(
  private val bookingSearchService: BookingSearchService,
  private val bookingSearchResultTransformer: BookingSearchResultTransformer,
) : BookingsApiDelegate {
  override fun bookingsSearchGet(
    xServiceName: ServiceName,
    status: BookingStatus?,
    sortOrder: SortOrder?,
    sortField: BookingSearchSortField?,
    page: Int?,
  ): ResponseEntity<BookingSearchResults> {
    val sortOrder = sortOrder ?: SortOrder.ascending
    val sortField = sortField ?: BookingSearchSortField.bookingCreatedAt

    val (results, metadata) = bookingSearchService.findBookings(xServiceName, status, sortOrder, sortField, page)

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(
        bookingSearchResultTransformer.transformDomainToApi(results),
      )
  }
}
