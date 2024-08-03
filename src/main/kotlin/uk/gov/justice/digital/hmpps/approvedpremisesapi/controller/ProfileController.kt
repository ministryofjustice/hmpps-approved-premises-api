package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ProfileApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.GetUserResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer

@Service
class ProfileController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
) : ProfileApiDelegate {
  override fun profileGet(xServiceName: ServiceName): ResponseEntity<User> {
    val userEntity = userService.getUserForRequest()

    return ResponseEntity(userTransformer.transformJpaToApi(userEntity, xServiceName), HttpStatus.OK)
  }

  override fun profileV2Get(xServiceName: ServiceName): ResponseEntity<ProfileResponse> {
    val username = userService.getDeliusUserNameForRequest()
    var getUserResponse = userService.getUserForProfile(username)
    if (getUserResponse.user != null && !getUserResponse.createdOnGet) {
      val updateResponse = userService.updateUserFromCommunityApi(getUserResponse.user!!, xServiceName)
      getUserResponse = (updateResponse as AuthorisableActionResult.Success<GetUserResponse>).entity
    }
    return ResponseEntity(userTransformer.transformProfileResponseToApi(username, getUserResponse, xServiceName), HttpStatus.OK)
  }
}
