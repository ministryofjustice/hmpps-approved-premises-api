/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.13.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
*/
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError

@RestController
interface BookingsApi {

  fun getDelegate(): BookingsApiDelegate = object : BookingsApiDelegate {}

  @Operation(
    tags = ["default"],
    summary = "Gets a booking",
    operationId = "bookingsBookingIdGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved booking", content = [Content(schema = Schema(implementation = Booking::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/bookings/{bookingId}"],
    produces = ["application/json"],
  )
  fun bookingsBookingIdGet(@Parameter(description = "ID of the booking", required = true) @PathVariable("bookingId") bookingId: java.util.UUID): ResponseEntity<Booking> = getDelegate().bookingsBookingIdGet(bookingId)

  @Operation(
    tags = ["default"],
    summary = "Searches for bookings with the given parameters",
    operationId = "bookingsSearchGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = BookingSearchResults::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/bookings/search"],
    produces = ["application/json", "application/problem+json"],
  )
  fun bookingsSearchGet(@RequestParam(value = "status", required = false) status: BookingStatus?, @RequestParam(value = "sortOrder", required = false) sortOrder: SortOrder?, @RequestParam(value = "sortField", required = false) sortField: BookingSearchSortField?, @RequestParam(value = "page", required = false) page: kotlin.Int?, @RequestParam(value = "crnOrName", required = false) crnOrName: kotlin.String?): ResponseEntity<BookingSearchResults> = getDelegate().bookingsSearchGet(status, sortOrder, sortField, page, crnOrName)
}
