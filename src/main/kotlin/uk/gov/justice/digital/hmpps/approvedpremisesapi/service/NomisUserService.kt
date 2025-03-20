package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisUserDetail
import java.util.*
import kotlin.jvm.optionals.getOrElse

@Service
class NomisUserService(
  private val httpAuthService: HttpAuthService,
  private val nomisUserRolesApiClient: NomisUserRolesApiClient,
  private val userRepository: NomisUserRepository,
) {
  fun getUserForRequest(): NomisUserEntity {
    val authenticatedPrincipal = httpAuthService.getNomisPrincipalOrThrow()
    val jwt = authenticatedPrincipal.token.tokenValue
    val username = authenticatedPrincipal.name

    return getUserForUsername(username, jwt)
  }

  fun getNomisUserByIdAndAddIfMissing(id: UUID): NomisUserEntity {
    return userRepository.findById(id).getOrElse {
      // TODO
      // this should call the nomis-user-roles api - /users/staff/{staffId} to get the staffDetail
      // and create a user if the user is found to be missing from this repository.
      // need to check permissions/roles before implementing
      throw RuntimeException("No user for id $id found")
    }
  }

  fun getNomisUserByStaffIdAndAddIfMissing(staffId: Long): NomisUserEntity {
    return userRepository.findByNomisStaffId(staffId) ?:
    // TODO
    // this should call the nomis-user-roles api - /users/staff/{staffId} to get the staffDetail
    // and create a user if the user is found to be missing from this repository.
    // need to check permissions/roles before implementing
    throw RuntimeException("No user for staffId $staffId found")
  }

  @Transactional
  fun getUserForUsername(username: String, jwt: String): NomisUserEntity {
    val nomisUserDetails = when (
      val nomisUserDetailResponse = nomisUserRolesApiClient.getUserDetails(jwt)
    ) {
      is ClientResult.Success -> nomisUserDetailResponse.body
      is ClientResult.Failure -> nomisUserDetailResponse.throwException()
    }

    val normalisedUsername = username.uppercase()
    val existingUser = userRepository.findByNomisUsername(normalisedUsername)

    if (existingUser != null) {
      if (existingUserDetailsHaveChanged(existingUser, nomisUserDetails)) {
        existingUser.email = nomisUserDetails.primaryEmail
        existingUser.activeCaseloadId = nomisUserDetails.activeCaseloadId
        userRepository.save(existingUser)
      }
      return existingUser
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
        activeCaseloadId = nomisUserDetails.activeCaseloadId,
      ),
    )
  }

  private fun existingUserDetailsHaveChanged(
    existingUser: NomisUserEntity,
    nomisUserDetails: NomisUserDetail,
  ): Boolean = (
    existingUser.email != nomisUserDetails.primaryEmail ||
      existingUser.activeCaseloadId != nomisUserDetails.activeCaseloadId
    )
}
