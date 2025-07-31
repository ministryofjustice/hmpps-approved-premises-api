package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.BookingsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingSearchResultTransformer

@Service
class BookingsController(
  private val cas3BookingSearchService: Cas3BookingSearchService,
  private val bookingSearchResultTransformer: BookingSearchResultTransformer,
) : BookingsApiDelegate {

  override fun bookingsSearchGet(
    status: BookingStatus?,
    sortOrder: SortOrder?,
    sortField: BookingSearchSortField?,
    page: Int?,
    crnOrName: String?,
  ): ResponseEntity<BookingSearchResults> {
    val sortOrder = sortOrder ?: SortOrder.ascending
    val sortField = sortField ?: BookingSearchSortField.bookingCreatedAt

    val (results, metadata) = cas3BookingSearchService.findBookings(status, sortOrder, sortField, page, crnOrName)

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(
        bookingSearchResultTransformer.transformDomainToApi(results),
      )
  }
}
