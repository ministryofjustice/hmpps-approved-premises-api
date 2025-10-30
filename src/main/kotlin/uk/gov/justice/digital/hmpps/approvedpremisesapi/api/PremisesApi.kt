
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTurnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.NewCas3Arrival

@RestController
interface PremisesApi {

  fun getDelegate(): PremisesApiDelegate = object : PremisesApiDelegate {}

  @Operation(
    tags = ["Premises"],
    summary = "Add a new premises",
    operationId = "premisesPost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "201", description = "new premises added", content = [Content(schema = Schema(implementation = Premises::class))]),
      ApiResponse(responseCode = "400", description = "invalid request", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/premises"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun premisesPost(@Parameter(description = "", required = true) @RequestBody body: NewPremises, @Parameter(description = "If given, persist the service name against this property", `in` = ParameterIn.HEADER, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?): ResponseEntity<Premises> = getDelegate().premisesPost(body, xServiceName)

  @Operation(
    tags = ["Operations on bookings"],
    summary = "Posts a change to the dates for a specified approved premises booking",
    operationId = "premisesPremisesIdBookingsBookingIdDateChangesPost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = DateChange::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
      ApiResponse(responseCode = "404", description = "invalid premises ID or booking ID", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/premises/{premisesId}/bookings/{bookingId}/date-changes"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun premisesPremisesIdBookingsBookingIdDateChangesPost(@Parameter(description = "ID of the premises the booking is related to", required = true) @PathVariable("premisesId") premisesId: java.util.UUID, @Parameter(description = "ID of the booking", required = true) @PathVariable("bookingId") bookingId: java.util.UUID, @Parameter(description = "details of the extension", required = true) @RequestBody body: NewDateChange): ResponseEntity<DateChange> = getDelegate().premisesPremisesIdBookingsBookingIdDateChangesPost(premisesId, bookingId, body)

  @Operation(
    tags = ["Operations on premises"],
    summary = "Returns a specific booking for an approved premises",
    operationId = "premisesPremisesIdBookingsBookingIdGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Booking::class))]),
      ApiResponse(responseCode = "404", description = "invalid premises ID", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/premises/{premisesId}/bookings/{bookingId}"],
    produces = ["application/json"],
  )
  fun premisesPremisesIdBookingsBookingIdGet(@Parameter(description = "ID of the premises the booking is related to", required = true) @PathVariable("premisesId") premisesId: java.util.UUID, @Parameter(description = "ID of the booking", required = true) @PathVariable("bookingId") bookingId: java.util.UUID): ResponseEntity<Booking> = getDelegate().premisesPremisesIdBookingsBookingIdGet(premisesId, bookingId)


  @Operation(
    tags = ["Premises"],
    summary = "Returns an approved premises",
    operationId = "premisesPremisesIdGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Premises::class))]),
      ApiResponse(responseCode = "404", description = "invalid premises ID", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/premises/{premisesId}"],
    produces = ["application/json"],
  )
  fun premisesPremisesIdGet(@Parameter(description = "ID of the premises to return", required = true) @PathVariable("premisesId") premisesId: java.util.UUID): ResponseEntity<Premises> = getDelegate().premisesPremisesIdGet(premisesId)


  @Operation(
    tags = ["Operations on premises"],
    summary = "Updates a premises",
    operationId = "premisesPremisesIdPut",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Premises::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    value = ["/premises/{premisesId}"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun premisesPremisesIdPut(@Parameter(description = "ID of the premises", required = true) @PathVariable("premisesId") premisesId: java.util.UUID, @Parameter(description = "Information to update the premises with", required = true) @RequestBody body: UpdatePremises): ResponseEntity<Premises> = getDelegate().premisesPremisesIdPut(premisesId, body)

 }
