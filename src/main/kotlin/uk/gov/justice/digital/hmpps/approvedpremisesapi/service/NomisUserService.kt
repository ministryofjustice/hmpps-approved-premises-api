package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import java.util.UUID

@Service
class NomisUserService(
  private val httpAuthService: HttpAuthService,
  private val nomisUserRolesApiClient: NomisUserRolesApiClient,
  private val userRepository: NomisUserRepository,
) {
  fun getUserForRequest(): NomisUserEntity {
    val authenticatedPrincipal = httpAuthService.getNomisPrincipalOrThrow()
    val username = authenticatedPrincipal.name

    return getUserForUsername(username)
  }

  fun getUserForUsername(username: String): NomisUserEntity {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByNomisUsername(normalisedUsername)
    if (existingUser != null) return existingUser

    val jwt = httpAuthService.getNomisPrincipalOrThrow().token.tokenValue
    val nomisUserDetails = when (
      val nomisUserDetailResponse = nomisUserRolesApiClient.getUserDetails(jwt)
    ) {
      is ClientResult.Success -> nomisUserDetailResponse.body
      is ClientResult.Failure -> nomisUserDetailResponse.throwException()
    }

    return userRepository.save(
      NomisUserEntity(
        id = UUID.randomUUID(),
        name = "${nomisUserDetails.firstName} ${nomisUserDetails.lastName}",
        nomisUsername = normalisedUsername,
        nomisStaffId = nomisUserDetails.staffId,
        accountType = nomisUserDetails.accountType,
        email = nomisUserDetails.primaryEmail,
        isEnabled = nomisUserDetails.enabled,
        isActive = nomisUserDetails.active,
        applications = mutableListOf(),
      ),
    )
  }
}
