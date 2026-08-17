package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer

@RestController
@Tag(name = "Auth")
class ProfileController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Operation(
    summary = "Returns information on the logged in user",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved information on user", content = [Content(schema = Schema(implementation = ProfileResponse::class))]),
    ],
  )
  @GetMapping(
    value = ["/profile/v2"],
    produces = ["application/json"],
  )
  @SuppressWarnings("TooGenericExceptionThrown")
  fun profileV2Get(
    @Parameter(
      description = "Filters the user details to those relevant to the specified service.",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader("X-Service-Name")
    xServiceName: ServiceName,
    @RequestParam readOnly: Boolean?,
  ): ResponseEntity<ProfileResponse> {
    val username = userService.getDeliusUserNameForRequest()
    val getUserResponse = userService.getUserForProfile(username)

    when (getUserResponse) {
      UserService.GetUserResponse.StaffRecordNotFound -> {
        log.info("On call to /profile/v2 staff record for $username not found")
      }

      is UserService.GetUserResponse.StaffProbationRegionNotSupported -> {
        throw RuntimeException("Probation region '${getUserResponse.unsupportedRegionId}' not supported for user '${username.uppercase()}'")
      }

      is UserService.GetUserResponse.Success -> {
        if (getUserResponse.createdOnGet) {
          log.info("On call to /profile/v2 user record for $username created")
        } else {
          log.info("On call to /profile/v2 user record for $username already exists")
        }
      }
    }

    val responseToReturn =
      if (getUserResponse is UserService.GetUserResponse.Success &&
        !getUserResponse.createdOnGet &&
        readOnly != true
      ) {
        log.info("Updating user record for $username")
        userService.updateUserFromDelius(getUserResponse.user, xServiceName)
      } else {
        getUserResponse
      }

    return ResponseEntity(userTransformer.transformProfileResponseToApi(username, responseToReturn, xServiceName), HttpStatus.OK)
  }
}
