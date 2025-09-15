package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.approvedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer

@Cas1Controller
@Tag(name = "CAS1 User Profile")
class Cas1ProfileController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Operation(summary = "Get User profile")
  @GetMapping(
    value = ["/profile"],
    produces = ["application/json"],
  )
  fun getUserProfile(
    @RequestParam readOnly: Boolean?,
  ): ResponseEntity<Cas1ProfileResponse> {
    val username = userService.getDeliusUserNameForRequest()
    val getUserResponse = userService.getExistingUserOrCreate(username)

    when (getUserResponse) {
      UserService.GetUserResponse.StaffRecordNotFound -> {
        log.info("On call to /cas1/profile staff record for $username not found")
      }
      is UserService.GetUserResponse.Success -> {
        if (getUserResponse.createdOnGet) {
          log.info("On call to /cas1/profile user record for $username created")
        } else {
          log.info("On call to /cas1/profile user record for $username already exists")
        }
      }

      is UserService.GetUserResponse.StaffProbationRegionNotSupported -> Cas1ProfileResponse.Cas1LoadError.unsupportedProbationRegion
    }

    val responseToReturn =
      if (getUserResponse is UserService.GetUserResponse.Success &&
        !getUserResponse.createdOnGet &&
        readOnly != true
      ) {
        log.info("Updating user record for $username")
        userService.updateUserFromDelius(getUserResponse.user, approvedPremises)
      } else {
        getUserResponse
      }

    return ResponseEntity(transformCas1ProfileResponseToApi(username, responseToReturn), HttpStatus.OK)
  }

  private fun transformCas1ProfileResponseToApi(userName: String, userResponse: UserService.GetUserResponse): Cas1ProfileResponse = when (userResponse) {
    UserService.GetUserResponse.StaffRecordNotFound -> Cas1ProfileResponse(userName, Cas1ProfileResponse.Cas1LoadError.staffRecordNotFound)
    is UserService.GetUserResponse.StaffProbationRegionNotSupported -> Cas1ProfileResponse(userName, Cas1ProfileResponse.Cas1LoadError.unsupportedProbationRegion)
    is UserService.GetUserResponse.Success -> Cas1ProfileResponse(userName, user = userTransformer.transformCas1JpaToApi(userResponse.user))
  }
}
