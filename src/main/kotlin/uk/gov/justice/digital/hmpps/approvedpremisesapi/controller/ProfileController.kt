package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ProfileApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer

@Service
class ProfileController(
  private val userService: UserService,
  private val userTransformer: UserTransformer,
  private val featureFlagService: FeatureFlagService,
) : ProfileApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun profileGet(xServiceName: ServiceName): ResponseEntity<User> {
    val userEntity = userService.getUserForRequest()

    return ResponseEntity(userTransformer.transformJpaToApi(userEntity, xServiceName), HttpStatus.OK)
  }

  override fun profileV2Get(xServiceName: ServiceName): ResponseEntity<ProfileResponse> {
    val username = userService.getDeliusUserNameForRequest()
    val getUserResponse = userService.getUserForProfile(username)

    when (getUserResponse) {
      UserService.GetUserResponse.StaffRecordNotFound -> {
        log.info("On call to /profile/v2 staff record for $username not found")
      }
      is UserService.GetUserResponse.Success -> {
        if (getUserResponse.createdOnGet) {
          log.info("On call to /profile/v2 user record for $username created")
        } else {
          log.info("On call to /profile/v2 user record for $username already exists")
        }
      }
    }

    val responseToReturn = if (
      getUserResponse is UserService.GetUserResponse.Success &&
      !getUserResponse.createdOnGet &&
      featureFlagService.isUseApAndDeliusToUpdateUsersEnabled()
    ) {
      log.info("Updating user record for $username")
      userService.updateUser(getUserResponse.user, xServiceName)
    } else {
      getUserResponse
    }

    return ResponseEntity(userTransformer.transformProfileResponseToApi(username, responseToReturn, xServiceName), HttpStatus.OK)
  }
}
