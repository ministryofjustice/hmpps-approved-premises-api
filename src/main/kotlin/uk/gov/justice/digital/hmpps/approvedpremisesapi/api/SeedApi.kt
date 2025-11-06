
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedRequest

@RestController
interface SeedApi {

  fun getDelegate(): SeedApiDelegate = object : SeedApiDelegate {}

  @Operation(
    tags = ["default"],
    summary = "Starts the data seeding process, can only be called from a local connection",
    operationId = "seedPost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "202", description = "successfully requested task"),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/seed"],
    consumes = ["application/json"],
  )
  fun seedPost(@Parameter(description = "", required = true) @RequestBody seedRequest: SeedRequest): ResponseEntity<Unit> = getDelegate().seedPost(seedRequest)
}
