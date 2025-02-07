package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CacheType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem

@RestController
interface CacheApi {

  fun getDelegate(): CacheApiDelegate = object : CacheApiDelegate {}

  @Operation(
    tags = ["default"],
    summary = "Clears the given cache, can only be called from a local connection",
    operationId = "cacheCacheNameDelete",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully cleared cache"),
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
    method = [RequestMethod.DELETE],
    value = ["/cache/{cacheName}"],
    produces = ["application/json"],
  )
  fun cacheCacheNameDelete(
    @Parameter(
      description = "",
      required = true,
      schema = Schema(allowableValues = ["qCodeStaffMembers", "userAccess", "staffDetails", "teamsManagingCase", "ukBankHolidays", "inmateDetails"]),
    ) @PathVariable("cacheName") cacheName: CacheType,
  ): ResponseEntity<Unit> {
    return getDelegate().cacheCacheNameDelete(cacheName)
  }
}
