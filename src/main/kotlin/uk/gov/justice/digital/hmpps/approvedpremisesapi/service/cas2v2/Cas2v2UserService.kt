package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import java.util.UUID

@Service
class Cas2v2UserService(
  private val httpAuthService: HttpAuthService,
  private val userRepository: Cas2v2UserRepository,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val nomisUserRolesApiClient: NomisUserRolesApiClient,
  private val manageUsersApiClient: ManageUsersApiClient,
) {
  fun ensureUserPersisted() {
    getUserForRequest()
  }

  fun getUserForRequest(): Cas2v2UserEntity {
    val authenticatedPrincipal = httpAuthService.getCas2v2AuthenticatedPrincipalOrThrow()
    val jwt = authenticatedPrincipal.token.tokenValue
    val username = authenticatedPrincipal.name
    val userType = Cas2v2UserType.fromString(authenticatedPrincipal.authenticationSource())

    return getUserForUsername(username, jwt, userType)
  }

  fun getUserForUsername(username: String, jwt: String, userType: Cas2v2UserType): Cas2v2UserEntity {
    val normalisedUsername = username.uppercase()

    val userEntity = when (userType) {
      Cas2v2UserType.NOMIS -> getEntityForNomisUser(normalisedUsername, jwt)
      Cas2v2UserType.DELIUS -> getEntityForDeliusUser(normalisedUsername)
      Cas2v2UserType.EXTERNAL -> getEntityForExternalUser(normalisedUsername, jwt)
    }

    return userEntity
  }

  fun requiresCaseLoadIdCheck(): Boolean {
    return !userForRequestHasRole(
      listOf(
        SimpleGrantedAuthority("ROLE_COURT_BAIL"),
        SimpleGrantedAuthority("ROLE_PRISON_BAIL"),
      ),
    )
  }

  fun userForRequestHasRole(grantedAuthorities: List<GrantedAuthority>): Boolean {
    val roles = getRolesForUserForRequest()
    return roles?.any { it in grantedAuthorities } ?: false
  }

  fun getRolesForUserForRequest(): MutableCollection<GrantedAuthority>? =
    httpAuthService.getCas2v2AuthenticatedPrincipalOrThrow().authorities

  private fun getExistingUser(username: String, userType: Cas2v2UserType): Cas2v2UserEntity? =
    userRepository.findByUsernameAndUserType(username, userType)

  private fun getEntityForNomisUser(username: String, jwt: String): Cas2v2UserEntity {
    val nomisUserDetails: NomisUserDetail = when (
      val nomisUserDetailResponse = nomisUserRolesApiClient.getUserDetails(jwt)
    ) {
      is ClientResult.Success -> nomisUserDetailResponse.body
      is ClientResult.Failure -> nomisUserDetailResponse.throwException()
    }

    val existingUser = getExistingUser(username, Cas2v2UserType.NOMIS)
    if (existingUser != null) {
      if (existingUser.email != nomisUserDetails.primaryEmail || existingUser.activeNomisCaseloadId != nomisUserDetails.activeCaseloadId) {
        existingUser.email = nomisUserDetails.primaryEmail
        existingUser.activeNomisCaseloadId = nomisUserDetails.activeCaseloadId

        return userRepository.save(existingUser)
      }

      return existingUser
    }

    return userRepository.save(
      Cas2v2UserEntity(
        id = UUID.randomUUID(),
        name = "${nomisUserDetails.firstName} ${nomisUserDetails.lastName}",
        username = username,
        nomisStaffId = nomisUserDetails.staffId,
        activeNomisCaseloadId = nomisUserDetails.activeCaseloadId,
        userType = Cas2v2UserType.NOMIS,
        email = nomisUserDetails.primaryEmail,
        isEnabled = nomisUserDetails.enabled,
        isActive = nomisUserDetails.active,
        deliusTeamCodes = null,
        deliusStaffCode = null,
      ),
    )
  }

  private fun getEntityForDeliusUser(username: String): Cas2v2UserEntity {
    val deliusUser: StaffDetail =
      when (val staffUserDetailsResponse = apDeliusContextApiClient.getStaffDetail(username)) {
        is ClientResult.Success<*> -> staffUserDetailsResponse.body as StaffDetail
        is ClientResult.Failure<*> -> staffUserDetailsResponse.throwException()
      }

    val existingUser = getExistingUser(username, Cas2v2UserType.DELIUS)
    if (existingUser != null) {
      val teamsDiffer = deliusUser.teamCodes().sorted() != existingUser.deliusTeamCodes?.sorted()
      if (deliusUser.email != existingUser.email || teamsDiffer) {
        existingUser.email = deliusUser.email
        existingUser.deliusTeamCodes = deliusUser.teamCodes()
        return userRepository.save(existingUser)
      }

      return existingUser
    }

    return userRepository.save(
      Cas2v2UserEntity(
        id = UUID.randomUUID(),
        name = "${deliusUser.name.forename} ${deliusUser.name.surname}",
        username = username,
        nomisStaffId = null,
        activeNomisCaseloadId = null,
        userType = Cas2v2UserType.DELIUS,
        email = deliusUser.email,
        isEnabled = deliusUser.active,
        isActive = deliusUser.active,
        deliusTeamCodes = deliusUser.teamCodes(),
        deliusStaffCode = deliusUser.code,
      ),
    )
  }

  private fun getEntityForExternalUser(username: String, jwt: String): Cas2v2UserEntity {
    val existingUser = getExistingUser(username, Cas2v2UserType.EXTERNAL)
    if (existingUser != null) return existingUser

    val externalUserDetailsResponse = manageUsersApiClient.getExternalUserDetails(username, jwt)

    val externalUserDetails = when (externalUserDetailsResponse) {
      is ClientResult.Success -> externalUserDetailsResponse.body
      is ClientResult.Failure -> externalUserDetailsResponse.throwException()
    }

    return userRepository.save(
      Cas2v2UserEntity(
        id = UUID.randomUUID(),
        name = "${externalUserDetails.firstName} ${externalUserDetails.lastName}",
        username = externalUserDetails.username,
        deliusTeamCodes = null,
        deliusStaffCode = null,
        nomisStaffId = null,
        activeNomisCaseloadId = null,
        userType = Cas2v2UserType.EXTERNAL,
        email = externalUserDetails.email,
        isEnabled = externalUserDetails.enabled,
        isActive = true,
      ),
    )
  }
}
