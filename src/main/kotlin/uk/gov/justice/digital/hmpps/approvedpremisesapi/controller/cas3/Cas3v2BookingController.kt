package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2.Cas3v2BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BookingSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2BookingController(
  private val bookingService: Cas3v2BookingService,
  private val bookingSearchService: Cas3v2BookingSearchService,
  private val bookingTransformer: Cas3BookingTransformer,
  private val bookingSearchResultTransformer: Cas3BookingSearchResultTransformer,
) {

  @GetMapping("/bookings/{bookingId}")
  fun bookingsBookingIdGet(@PathVariable bookingId: UUID): ResponseEntity<Cas3Booking> {
    val bookingAndPersonsResult = bookingService.getBooking(bookingId, premisesId = null)
    val bookingAndPersons = extractEntityFromCasResult(bookingAndPersonsResult)

    val apiBooking = bookingTransformer.transformJpaToApi(
      bookingAndPersons.booking,
      bookingAndPersons.personInfo,
    )

    return ResponseEntity.ok(apiBooking)
  }

  @PaginationHeaders
  @GetMapping("/bookings/search")
  fun bookingsSearch(
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
}
