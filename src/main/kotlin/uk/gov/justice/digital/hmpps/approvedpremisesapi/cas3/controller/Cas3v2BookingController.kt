package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2BookingController(
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val bookingService: Cas3v2BookingService,
  private val bookingSearchService: Cas3v2BookingSearchService,
  private val bookingTransformer: Cas3BookingTransformer,
  private val bookingSearchResultTransformer: Cas3BookingSearchResultTransformer,
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
}
