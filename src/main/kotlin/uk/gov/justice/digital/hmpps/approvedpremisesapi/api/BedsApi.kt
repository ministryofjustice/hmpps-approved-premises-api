package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError

@RestController
interface BedsApi {

  fun getDelegate(): BedsApiDelegate = object : BedsApiDelegate {}

  @Operation(
    tags = ["default"],
    summary = "Searches for available Beds within the given parameters",
    operationId = "bedsSearchPost",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = BedSearchResults::class))],
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
    method = [RequestMethod.POST],
    value = ["/beds/search"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun bedsSearchPost(
    @Parameter(
      description = "",
      required = true,
    ) @RequestBody temporaryAccommodationBedSearchParameters: TemporaryAccommodationBedSearchParameters,
  ): ResponseEntity<BedSearchResults> {
    return getDelegate().bedsSearchPost(temporaryAccommodationBedSearchParameters)
  }
}
