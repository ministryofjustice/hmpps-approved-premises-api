package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TypedUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserTypeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesForRequesterApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import java.util.UUID

@Service
class Cas2UserService(
  private val httpAuthService: HttpAuthService,
  private val cas2UserRepository: Cas2UserRepository,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val nomisUserRolesApiClient: NomisUserRolesForRequesterApiClient,
  private val manageUsersApiClient: ManageUsersApiClient,
  private val cas2UserTransformer: Cas2UserTransformer,
) {
  fun ensureUserPersisted() {
    getUserForRequest()
  }

  fun getUserForRequest(): Cas2TypedUser {
    val authenticatedPrincipal = httpAuthService.getCas2v2AuthenticatedPrincipalOrThrow()
    val jwt = authenticatedPrincipal.token.tokenValue
    val username = authenticatedPrincipal.name
    val userType = Cas2UserType.fromString(authenticatedPrincipal.authenticationSource())

    return getUserForUsername(username, jwt, userType)
  }

  fun getUserDtoForRequest(): CasResult<Cas2UserDto> {
    val user = getUserForRequest()

    return CasResult.Success(
      Cas2UserDto(
        username = user.username,
        type = Cas2UserTypeDto.valueOf(user.userType.name),
        deliusUserInfo = if (user is Cas2TypedUser.Delius) user.deliusUserInfo else null,
      ),
    )
  }

  private fun getUserForUsername(username: String, jwt: String, userType: Cas2UserType): Cas2TypedUser {
    val normalisedUsername = username.uppercase()

    val userEntity = when (userType) {
      Cas2UserType.NOMIS -> getTypedNomisUser(normalisedUsername, jwt)
      Cas2UserType.DELIUS -> getTypedDeliusUser(normalisedUsername)
      Cas2UserType.EXTERNAL -> getTypedExternalUser(normalisedUsername, jwt)
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
    return roles.any { it in grantedAuthorities }
  }

  private fun getRolesForUserForRequest(): MutableCollection<GrantedAuthority> = httpAuthService.getCas2v2AuthenticatedPrincipalOrThrow().authorities

  private fun getExistingUser(username: String, userType: Cas2UserType): Cas2UserEntity? = cas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, userType, Cas2ServiceOrigin.BAIL)

  private fun getTypedNomisUser(username: String, jwt: String): Cas2TypedUser.Nomis {
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

        return cas2UserTransformer.transformJpaToTypedNomisUser(cas2UserRepository.save(existingUser))
      }

      return cas2UserTransformer.transformJpaToTypedNomisUser(existingUser)
    }

    cas2UserRepository.createCas2User(
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
        serviceOrigin = Cas2ServiceOrigin.BAIL,
        nomisAccountType = nomisUserDetails.accountType,
      ),
    )
    return cas2UserTransformer.transformJpaToTypedNomisUser(getExistingUser(username, Cas2UserType.NOMIS)!!)
  }

  private fun getTypedDeliusUser(username: String): Cas2TypedUser.Delius {
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
        return cas2UserTransformer.transformJpaToTypedDeliusUser(
          cas2UserRepository.save(existingUser),
          deliusUser,
        )
      }

      return cas2UserTransformer.transformJpaToTypedDeliusUser(existingUser, deliusUser)
    }

    cas2UserRepository.createCas2User(
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
        serviceOrigin = Cas2ServiceOrigin.BAIL,
      ),
    )
    return cas2UserTransformer.transformJpaToTypedDeliusUser(
      getExistingUser(username, Cas2UserType.DELIUS)!!,
      deliusUser,
    )
  }

  private fun getTypedExternalUser(username: String, jwt: String): Cas2TypedUser.External {
    val existingUser = getExistingUser(username, Cas2UserType.EXTERNAL)
    if (existingUser != null) return cas2UserTransformer.transformJpaToTypedExternalUser(existingUser)

    val externalUserDetailsResponse = manageUsersApiClient.getExternalUserDetails(username, jwt)

    val externalUserDetails = when (externalUserDetailsResponse) {
      is ClientResult.Success -> externalUserDetailsResponse.body
      is ClientResult.Failure -> externalUserDetailsResponse.throwException()
    }

    cas2UserRepository.createCas2User(
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
        serviceOrigin = Cas2ServiceOrigin.BAIL,
        externalType = "NACRO",
      ),
    )
    return cas2UserTransformer.transformJpaToTypedExternalUser(getExistingUser(username, Cas2UserType.EXTERNAL)!!)
  }
}
