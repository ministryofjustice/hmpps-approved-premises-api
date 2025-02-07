package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.util.UUID

@RestController
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class BookingsController(
  private val bookingSearchService: BookingSearchService,
  private val bookingSearchResultTransformer: BookingSearchResultTransformer,
  private val bookingService: BookingService,
  private val bookingTransformer: BookingTransformer,
) {

  @Operation(
    tags = ["default"],
    summary = "Gets a booking",
    operationId = "bookingsBookingIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successfully retrieved booking",
        content = [Content(schema = Schema(implementation = Booking::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/bookings/{bookingId}"],
    produces = ["application/json"],
  )
  fun bookingsBookingIdGet(bookingId: UUID): ResponseEntity<Booking> {
    val bookingAndPersons = when (val result = bookingService.getBooking(bookingId)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(bookingId, result.entityType!!)
      is AuthorisableActionResult.Success -> result.entity
    }

    val apiBooking = bookingTransformer.transformJpaToApi(
      bookingAndPersons.booking,
      bookingAndPersons.personInfo,
    )

    return ResponseEntity.ok(apiBooking)
  }

  @Operation(
    tags = ["default"],
    summary = "Searches for bookings with the given parameters",
    operationId = "bookingsSearchGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = BookingSearchResults::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "invalid params",
        content = [Content(schema = Schema(implementation = ValidationError::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/bookings/search"],
    produces = ["application/json", "application/problem+json"],
  )
  fun bookingsSearchGet(
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
