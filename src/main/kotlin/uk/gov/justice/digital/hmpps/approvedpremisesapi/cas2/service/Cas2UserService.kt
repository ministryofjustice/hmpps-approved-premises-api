package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import jakarta.transaction.Transactional
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesForRequesterApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisStaffInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import java.util.UUID

@Service
class Cas2UserService(
  private val httpAuthService: HttpAuthService,
  private val nomisUserRolesApiClient: NomisUserRolesApiClient,
  private val nomisUserRolesForRequesterApiClient: NomisUserRolesForRequesterApiClient,
  private val nomisUserRepository: NomisUserRepository,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val manageUsersApiClient: ManageUsersApiClient,
  private val cas2UserRepository: Cas2UserRepository,
) {
  // BAIL-WIP When we migrate, remove this method and force all calls to getCas2UserForRequest
  fun getUserForRequest(): NomisUserEntity {
    val authenticatedPrincipal = httpAuthService.getNomisPrincipalOrThrow()
    val jwt = authenticatedPrincipal.token.tokenValue
    val username = authenticatedPrincipal.name

    return getNomisUserForUsername(username, jwt)
  }

  fun getCas2UserForRequest(): Cas2UserEntity {
    val authenticatedPrincipal = httpAuthService.getPrincipalOrThrow(listOf("nomis", "auth", "delius"))
    val jwt = authenticatedPrincipal.token.tokenValue
    val username = authenticatedPrincipal.name
    val userType = Cas2v2UserType.fromString(authenticatedPrincipal.authenticationSource())

    return getCas2UserForUsername(username, jwt, userType)
  }

  fun getNomisUserById(id: UUID) = nomisUserRepository.findById(id).orElseThrow { NotFoundProblem(id, "NomisUser") }

  fun getUserByStaffId(staffId: Long): NomisUserEntity {
    val userDetails = nomisUserRepository.findByNomisStaffId(staffId)
    if (userDetails != null) {
      return userDetails
    }
    val userStaffInformation = getUserStaffInformation(staffId)
    val normalisedUsername = userStaffInformation.generalAccount.username.uppercase()
    val userDetail = getUserDetail(username = normalisedUsername)
    return ensureUserExists(username = normalisedUsername, userDetail)
  }

  private fun getUserDetail(username: String): NomisUserDetail = when (
    val nomsStaffInformationResponse = nomisUserRolesApiClient.getUserDetails(username)
  ) {
    is ClientResult.Success -> nomsStaffInformationResponse.body
    is ClientResult.Failure -> nomsStaffInformationResponse.throwException()
  }

  private fun getUserStaffInformation(nomisStaffId: Long): NomisStaffInformation = when (
    val nomsStaffInformationResponse = nomisUserRolesApiClient.getUserStaffInformation(nomisStaffId)
  ) {
    is ClientResult.Success -> nomsStaffInformationResponse.body
    is ClientResult.Failure -> nomsStaffInformationResponse.throwException()
  }

  @Transactional(value = Transactional.TxType.REQUIRES_NEW)
  fun getNomisUserForUsername(username: String, jwt: String): NomisUserEntity {
    val nomisUserDetails = when (
      val nomisUserDetailResponse = nomisUserRolesForRequesterApiClient.getUserDetailsForMe(jwt)
    ) {
      is ClientResult.Success -> nomisUserDetailResponse.body
      is ClientResult.Failure -> nomisUserDetailResponse.throwException()
    }

    val normalisedUsername = username.uppercase()
    val existingUser = nomisUserRepository.findByNomisUsername(normalisedUsername)

    if (existingUser != null) {
      if (existingUserDetailsHaveChanged(existingUser, nomisUserDetails)) {
        existingUser.email = nomisUserDetails.primaryEmail
        existingUser.activeCaseloadId = nomisUserDetails.activeCaseloadId
        nomisUserRepository.save(existingUser)
      }
      return existingUser
    }
    return ensureUserExists(username = normalisedUsername, nomisUserDetails)
  }

  private fun ensureUserExists(
    username: String,
    nomisUserDetails: NomisUserDetail,
  ): NomisUserEntity {
    return nomisUserRepository.findByNomisUsername(username) ?: try {
      nomisUserRepository.save(
        NomisUserEntity(
          id = UUID.randomUUID(),
          name = "${nomisUserDetails.firstName} ${nomisUserDetails.lastName}",
          nomisUsername = username,
          nomisStaffId = nomisUserDetails.staffId,
          accountType = nomisUserDetails.accountType,
          email = nomisUserDetails.primaryEmail,
          isEnabled = nomisUserDetails.enabled,
          isActive = nomisUserDetails.active,
          activeCaseloadId = nomisUserDetails.activeCaseloadId,
        ),
      )
    } catch (ex: DataIntegrityViolationException) {
      return nomisUserRepository.findByNomisUsername(username)
        ?: throw IllegalStateException("User creation failed and username $username not found", ex)
    }
  }

  private fun existingUserDetailsHaveChanged(
    existingUser: NomisUserEntity,
    nomisUserDetails: NomisUserDetail,
  ): Boolean = (
    existingUser.email != nomisUserDetails.primaryEmail ||
      existingUser.activeCaseloadId != nomisUserDetails.activeCaseloadId
    )

  fun getCas2UserForUsername(username: String, jwt: String, userType: Cas2v2UserType): Cas2UserEntity {
    val normalisedUsername = username.uppercase()

    val userEntity = when (userType) {
      Cas2v2UserType.NOMIS -> getCas2UserEntityForNomisUser(normalisedUsername, jwt)
      Cas2v2UserType.DELIUS -> getCas2UserEntityForDeliusUser(normalisedUsername)
      Cas2v2UserType.EXTERNAL -> getCas2UserEntityForExternalUser(normalisedUsername, jwt)
    }

    return userEntity
  }

  private fun getExistingCas2User(username: String, userType: Cas2UserType): Cas2UserEntity? = cas2UserRepository.findByUsernameAndUserType(username, userType)

  private fun getCas2UserEntityForNomisUser(username: String, jwt: String): Cas2UserEntity {
    val nomisUserDetails: NomisUserDetail = when (
      val nomisUserDetailResponse = nomisUserRolesForRequesterApiClient.getUserDetailsForMe(jwt)
    ) {
      is ClientResult.Success -> nomisUserDetailResponse.body
      is ClientResult.Failure -> nomisUserDetailResponse.throwException()
    }

    val existingUser = getExistingCas2User(username, Cas2UserType.NOMIS)
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

  private fun getCas2UserEntityForDeliusUser(username: String): Cas2UserEntity {
    val deliusUser: StaffDetail =
      when (val staffUserDetailsResponse = apDeliusContextApiClient.getStaffDetail(username)) {
        is ClientResult.Success<*> -> staffUserDetailsResponse.body as StaffDetail
        is ClientResult.Failure<*> -> staffUserDetailsResponse.throwException()
      }

    val existingUser = getExistingCas2User(username, Cas2UserType.DELIUS)
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

  private fun getCas2UserEntityForExternalUser(username: String, jwt: String): Cas2UserEntity {
    val existingUser = getExistingCas2User(username, Cas2UserType.EXTERNAL)
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
