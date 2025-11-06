
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

@RestController
interface ProfileApi {

  fun getDelegate(): ProfileApiDelegate = object : ProfileApiDelegate {}

  @Operation(
    tags = ["Auth"],
    summary = "Returns information on the logged in user",
    operationId = "profileV2Get",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved information on user", content = [Content(schema = Schema(implementation = ProfileResponse::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/profile/v2"],
    produces = ["application/json"],
  )
  fun profileV2Get(@Parameter(description = "Filters the user details to those relevant to the specified service.", `in` = ParameterIn.HEADER, required = true, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = true) xServiceName: ServiceName, @RequestParam(value = "readOnly", required = false) readOnly: kotlin.Boolean?): ResponseEntity<ProfileResponse> = getDelegate().profileV2Get(xServiceName, readOnly)
}
