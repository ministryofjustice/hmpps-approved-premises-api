package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import java.util.UUID

@Service
class UserService(
  private val httpAuthService: HttpAuthService,
  private val communityApiClient: CommunityApiClient,
  private val userRepository: UserRepository
) {
  fun getUserForRequest(): UserEntity {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val existingUser = userRepository.findByDeliusUsername(username)
    if (existingUser != null) return existingUser

    val staffUserDetailsResponse = communityApiClient.getStaffUserDetails(username)

    val staffUserDetails = when (staffUserDetailsResponse) {
      is ClientResult.Success -> staffUserDetailsResponse.body
      is ClientResult.Failure -> staffUserDetailsResponse.throwException()
    }

    return userRepository.save(
      UserEntity(
        id = UUID.randomUUID(),
        name = "${staffUserDetails.staff.forenames} ${staffUserDetails.staff.surname}",
        deliusUsername = username,
        deliusStaffIdentifier = staffUserDetails.staffIdentifier,
        applications = mutableListOf(),
        roles = mutableListOf(),
        qualifications = mutableListOf()
      )
    )
  }
}
