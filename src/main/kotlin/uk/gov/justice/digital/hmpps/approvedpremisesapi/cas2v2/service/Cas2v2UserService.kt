package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesForRequesterApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import java.util.UUID

@Service
class Cas2v2UserService(
  private val httpAuthService: HttpAuthService,
  private val cas2UserRepository: Cas2UserRepository,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val nomisUserRolesApiClient: NomisUserRolesForRequesterApiClient,
  private val manageUsersApiClient: ManageUsersApiClient,
) {
  fun ensureUserPersisted() {
    getUserForRequest()
  }

  fun getUserForRequest(): Cas2UserEntity {
    val authenticatedPrincipal = httpAuthService.getCas2v2AuthenticatedPrincipalOrThrow()
    val jwt = authenticatedPrincipal.token.tokenValue
    val username = authenticatedPrincipal.name
    val userType = Cas2UserType.fromString(authenticatedPrincipal.authenticationSource())

    return getUserForUsername(username, jwt, userType)
  }

  fun getUserForUsername(username: String, jwt: String, userType: Cas2UserType): Cas2UserEntity {
    val normalisedUsername = username.uppercase()

    val userEntity = when (userType) {
      Cas2UserType.NOMIS -> getEntityForNomisUser(normalisedUsername, jwt)
      Cas2UserType.DELIUS -> getEntityForDeliusUser(normalisedUsername)
      Cas2UserType.EXTERNAL -> getEntityForExternalUser(normalisedUsername, jwt)
    }

    return userEntity
  }

  fun requiresCaseLoadIdCheck(): Boolean = !userForRequestHasRole(
    listOf(
      SimpleGrantedAuthority("ROLE_CAS2_COURT_BAIL_REFERRER"),
      SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER"),
    ),
  )

  fun userForRequestHasRole(grantedAuthorities: List<GrantedAuthority>): Boolean {
    val roles = getRolesForUserForRequest()
    return roles?.any { it in grantedAuthorities } ?: false
  }

  fun getRolesForUserForRequest(): MutableCollection<GrantedAuthority>? = httpAuthService.getCas2v2AuthenticatedPrincipalOrThrow().authorities

  private fun getExistingUser(username: String, userType: Cas2UserType): Cas2UserEntity? = cas2UserRepository.findByUsernameAndUserType(username, userType)

  private fun getEntityForNomisUser(username: String, jwt: String): Cas2UserEntity {
    val nomisUserDetails: NomisUserDetail = when (
      val nomisUserDetailResponse = nomisUserRolesApiClient.getUserDetailsForMe(jwt)
    ) {
      is ClientResult.Success -> nomisUserDetailResponse.body
      is ClientResult.Failure -> nomisUserDetailResponse.throwException()
    }

    val existingUser = getExistingUser(username, Cas2UserType.NOMIS)
    if (existingUser != null) {
      if (existingUser.email != nomisUserDetails.primaryEmail || existingUser.activeNomisCaseloadId != nomisUserDetails.activeCaseloadId) {
        existingUser.email = nomisUserDetails.primaryEmail
        existingUser.activeNomisCaseloadId = nomisUserDetails.activeCaseloadId

        return cas2UserRepository.save(existingUser)
      }

      return existingUser
    }

    return cas2UserRepository.save(
      Cas2UserEntity(
        id = UUID.randomUUID(),
        name = "${nomisUserDetails.firstName} ${nomisUserDetails.lastName}",
        username = username,
        nomisStaffId = nomisUserDetails.staffId,
        activeNomisCaseloadId = nomisUserDetails.activeCaseloadId,
        userType = Cas2UserType.NOMIS,
        email = nomisUserDetails.primaryEmail,
        isEnabled = nomisUserDetails.enabled,
        isActive = nomisUserDetails.active,
        deliusTeamCodes = null,
        deliusStaffCode = null,
      ),
    )
  }

  private fun getEntityForDeliusUser(username: String): Cas2UserEntity {
    val deliusUser: StaffDetail =
      when (val staffUserDetailsResponse = apDeliusContextApiClient.getStaffDetail(username)) {
        is ClientResult.Success<*> -> staffUserDetailsResponse.body as StaffDetail
        is ClientResult.Failure<*> -> staffUserDetailsResponse.throwException()
      }

    val existingUser = getExistingUser(username, Cas2UserType.DELIUS)
    if (existingUser != null) {
      val teamsDiffer = deliusUser.teamCodes().sorted() != existingUser.deliusTeamCodes?.sorted()
      if (deliusUser.email != existingUser.email || teamsDiffer) {
        existingUser.email = deliusUser.email
        existingUser.deliusTeamCodes = deliusUser.teamCodes()
        return cas2UserRepository.save(existingUser)
      }

      return existingUser
    }

    return cas2UserRepository.save(
      Cas2UserEntity(
        id = UUID.randomUUID(),
        name = "${deliusUser.name.forename} ${deliusUser.name.surname}",
        username = username,
        nomisStaffId = null,
        activeNomisCaseloadId = null,
        userType = Cas2UserType.DELIUS,
        email = deliusUser.email,
        isEnabled = deliusUser.active,
        isActive = deliusUser.active,
        deliusTeamCodes = deliusUser.teamCodes(),
        deliusStaffCode = deliusUser.code,
      ),
    )
  }

  private fun getEntityForExternalUser(username: String, jwt: String): Cas2UserEntity {
    val existingUser = getExistingUser(username, Cas2UserType.EXTERNAL)
    if (existingUser != null) return existingUser

    val externalUserDetailsResponse = manageUsersApiClient.getExternalUserDetails(username, jwt)

    val externalUserDetails = when (externalUserDetailsResponse) {
      is ClientResult.Success -> externalUserDetailsResponse.body
      is ClientResult.Failure -> externalUserDetailsResponse.throwException()
    }

    return cas2UserRepository.save(
      Cas2UserEntity(
        id = UUID.randomUUID(),
        name = "${externalUserDetails.firstName} ${externalUserDetails.lastName}",
        username = externalUserDetails.username,
        deliusTeamCodes = null,
        deliusStaffCode = null,
        nomisStaffId = null,
        activeNomisCaseloadId = null,
        userType = Cas2UserType.EXTERNAL,
        email = externalUserDetails.email,
        isEnabled = externalUserDetails.enabled,
        isActive = true,
      ),
    )
  }
}
