package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ProfileApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer

@Service
class ProfileController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
) : ProfileApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun profileGet(xServiceName: ServiceName): ResponseEntity<User> {
    val userEntity = userService.getUserForRequest()

    return ResponseEntity(userTransformer.transformJpaToApi(userEntity, xServiceName), HttpStatus.OK)
  }

  override fun profileV2Get(xServiceName: ServiceName): ResponseEntity<ProfileResponse> {
    val username = userService.getDeliusUserNameForRequest()
    val getUserResponse = userService.getUserForProfile(username)

    val responseToReturn = if (getUserResponse is UserService.GetUserResponse.Success && !getUserResponse.createdOnGet) {
      log.info("On call to /profile/v2 user record for $username already exists, so will update")
      userService.updateUserFromCommunityApi(getUserResponse.user, xServiceName)
    } else {
      log.info("On call to /profile/v2 user record for $username was created")
      getUserResponse
    }

    return ResponseEntity(userTransformer.transformProfileResponseToApi(username, responseToReturn, xServiceName), HttpStatus.OK)
  }
}
