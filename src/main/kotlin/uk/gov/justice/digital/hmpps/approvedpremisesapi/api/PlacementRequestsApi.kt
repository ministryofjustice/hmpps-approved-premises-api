
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade

@RestController
interface PlacementRequestsApi {

  fun getDelegate(): PlacementRequestsApiDelegate = object : PlacementRequestsApiDelegate {}

  @Operation(
    tags = ["Placement requests"],
    summary = "Records that an attempt to match was made but no suitable Beds could be found",
    operationId = "placementRequestsIdBookingNotMadePost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully recorded that a Booking could not be made", content = [Content(schema = Schema(implementation = BookingNotMade::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/placement-requests/{id}/booking-not-made"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun placementRequestsIdBookingNotMadePost(@Parameter(description = "ID of the placement request", required = true) @PathVariable("id") id: java.util.UUID, @Parameter(description = "Details about the failure to match", required = true) @RequestBody newBookingNotMade: NewBookingNotMade): ResponseEntity<BookingNotMade> = getDelegate().placementRequestsIdBookingNotMadePost(id, newBookingNotMade)
}
