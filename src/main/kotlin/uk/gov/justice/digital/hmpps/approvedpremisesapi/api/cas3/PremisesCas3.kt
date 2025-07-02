package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSortBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3UpdateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.*


import kotlin.collections.List

@RestController
interface PremisesCas3 {

    fun getDelegate(): PremisesCas3Delegate = object: PremisesCas3Delegate {}

    @Operation(
        tags = ["Bedspaces",],
        summary = "Adds a new bedspace for a premises",
        operationId = "createBedspace",
        description = """""",
        responses = [
            ApiResponse(responseCode = "201", description = "successful operation", content = [Content(schema = Schema(implementation = Cas3Bedspace::class))]),
            ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
            ApiResponse(responseCode = "404", description = "invalid premises ID", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.POST],
            value = ["/premises/{premisesId}/bedspaces"],
            produces = ["application/json", "application/problem+json"],
            consumes = ["application/json"]
    )
    fun createBedspace(@Parameter(description = "ID of the premises the bedspace is in", required = true) @PathVariable("premisesId") premisesId: java.util.UUID,@Parameter(description = "details of the new bedspace", required = true) @RequestBody body: Cas3NewBedspace): ResponseEntity<Cas3Bedspace> {
        return getDelegate().createBedspace(premisesId, body)
    }

    @Operation(
        tags = ["Premises",],
        summary = "Add a new premises",
        operationId = "createPremises",
        description = """""",
        responses = [
            ApiResponse(responseCode = "201", description = "new premises added", content = [Content(schema = Schema(implementation = Cas3Premises::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.POST],
            value = ["/premises"],
            produces = ["application/json"],
            consumes = ["application/json"]
    )
    fun createPremises(@Parameter(description = "", required = true) @RequestBody body: Cas3NewPremises): ResponseEntity<Cas3Premises> {
        return getDelegate().createPremises(body)
    }

    @Operation(
        tags = ["default",],
        summary = "Returns a specific bedspace for a premises",
        operationId = "getPremisesBedspace",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas3Bedspace::class))]),
            ApiResponse(responseCode = "404", description = "invalid premises ID or bedspace ID", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.GET],
            value = ["/premises/{premisesId}/bedspaces/{bedspaceId}"],
            produces = ["application/json"]
    )
    fun getPremisesBedspace(@Parameter(description = "ID of the premises the bedspace is in", required = true) @PathVariable("premisesId") premisesId: java.util.UUID,@Parameter(description = "ID of the bedspace to get", required = true) @PathVariable("bedspaceId") bedspaceId: java.util.UUID): ResponseEntity<Cas3Bedspace> {
        return getDelegate().getPremisesBedspace(premisesId, bedspaceId)
    }

    @Operation(
        tags = ["Bedspaces",],
        summary = "Lists all bedspaces for the given premises",
        operationId = "getPremisesBedspaces",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas3Bedspaces::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.GET],
            value = ["/premises/{premisesId}/bedspaces"],
            produces = ["application/json"]
    )
    fun getPremisesBedspaces(@Parameter(description = "ID of the premises to list the bedspaces for", required = true) @PathVariable("premisesId") premisesId: java.util.UUID): ResponseEntity<Cas3Bedspaces> {
        return getDelegate().getPremisesBedspaces(premisesId)
    }

    @Operation(
        tags = ["Premises",],
        summary = "Returns a premises",
        operationId = "getPremisesById",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas3Premises::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.GET],
            value = ["/premises/{premisesId}"],
            produces = ["application/json"]
    )
    fun getPremisesById(@Parameter(description = "ID of the premises to return", required = true) @PathVariable("premisesId") premisesId: java.util.UUID): ResponseEntity<Cas3Premises> {
        return getDelegate().getPremisesById(premisesId)
    }

    @Operation(
        tags = ["Operations on premises",],
        summary = "Returns all future bookings for a premises for a given statuses",
        operationId = "getPremisesFutureBookings",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = FutureBooking::class)))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.GET],
            value = ["/premises/{premisesId}/future-bookings"],
            produces = ["application/json"]
    )
    fun getPremisesFutureBookings(@Parameter(description = "ID of the premises to get bookings for", required = true) @PathVariable("premisesId") premisesId: java.util.UUID, @RequestParam(value = "statuses", required = true) statuses: kotlin.collections.List<BookingStatus>): ResponseEntity<List<FutureBooking>> {
        return getDelegate().getPremisesFutureBookings(premisesId, statuses)
    }

    @Operation(
        tags = ["Premises",],
        summary = "Returns a list of premises",
        operationId = "getPremisesSummary",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = Cas3PremisesSummary::class)))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.GET],
            value = ["/premises/summary"],
            produces = ["application/json"]
    )
    fun getPremisesSummary( @RequestParam(value = "postcodeOrAddress", required = false) postcodeOrAddress: kotlin.String?, @RequestParam(value = "sortBy", required = false) sortBy: Cas3PremisesSortBy?): ResponseEntity<List<Cas3PremisesSummary>> {
        return getDelegate().getPremisesSummary(postcodeOrAddress, sortBy)
    }

    @Operation(
        tags = ["Operations on bookings",],
        summary = "Posts a departure to a specified booking",
        operationId = "postPremisesBookingDeparture",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas3Departure::class))]),
            ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
            ApiResponse(responseCode = "404", description = "invalid premises ID or booking ID", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.POST],
            value = ["/premises/{premisesId}/bookings/{bookingId}/departures"],
            produces = ["application/json", "application/problem+json"],
            consumes = ["application/json"]
    )
    fun postPremisesBookingDeparture(@Parameter(description = "ID of the premises the booking is related to", required = true) @PathVariable("premisesId") premisesId: java.util.UUID,@Parameter(description = "ID of the booking", required = true) @PathVariable("bookingId") bookingId: java.util.UUID,@Parameter(description = "details of the departure", required = true) @RequestBody body: Cas3NewDeparture): ResponseEntity<Cas3Departure> {
        return getDelegate().postPremisesBookingDeparture(premisesId, bookingId, body)
    }

    @Operation(
        tags = ["Premises",],
        summary = "Searches for premises with the given parameters",
        operationId = "searchPremises",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas3PremisesSearchResults::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.GET],
            value = ["/premises/search"],
            produces = ["application/json"]
    )
    fun searchPremises( @RequestParam(value = "postcodeOrAddress", required = false) postcodeOrAddress: kotlin.String?, @RequestParam(value = "premisesStatus", required = false) premisesStatus: Cas3PremisesStatus?): ResponseEntity<Cas3PremisesSearchResults> {
        return getDelegate().searchPremises(postcodeOrAddress, premisesStatus)
    }

    @Operation(
        tags = ["default",],
        summary = "Updates a bedspace",
        operationId = "updateBedspace",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas3Bedspace::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.PUT],
            value = ["/premises/{premisesId}/bedspaces/{bedspaceId}"],
            produces = ["application/json"],
            consumes = ["application/json"]
    )
    fun updateBedspace(@Parameter(description = "ID of the premises where the bedspace is in", required = true) @PathVariable("premisesId") premisesId: java.util.UUID,@Parameter(description = "ID of the bedspace to update", required = true) @PathVariable("bedspaceId") bedspaceId: java.util.UUID,@Parameter(description = "Information to update the bedspace with", required = true) @RequestBody cas3UpdateBedspace: Cas3UpdateBedspace): ResponseEntity<Cas3Bedspace> {
        return getDelegate().updateBedspace(premisesId, bedspaceId, cas3UpdateBedspace)
    }
}
