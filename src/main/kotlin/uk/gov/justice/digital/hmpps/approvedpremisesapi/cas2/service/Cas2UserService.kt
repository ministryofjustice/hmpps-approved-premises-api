package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesForRequesterApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles.NomisStaffInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2UserService(
  private val httpAuthService: HttpAuthService,
  private val nomisUserRolesApiClient: NomisUserRolesApiClient,
  private val nomisUserRolesForRequesterApiClient: NomisUserRolesForRequesterApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val manageUsersApiClient: ManageUsersApiClient,
  private val cas2UserRepository: Cas2UserRepository,
) {
  fun getUserForRequest(serviceOrigin: Cas2ServiceOrigin): Cas2UserEntity {
    val authenticatedPrincipal = httpAuthService.getPrincipalOrThrow(listOf("nomis", "auth", "delius"))
    val jwt = authenticatedPrincipal.token.tokenValue
    val username = authenticatedPrincipal.name
    val userType = Cas2UserType.fromString(authenticatedPrincipal.authenticationSource())

    return getCas2UserForUsername(username, jwt, userType, serviceOrigin)
  }

  fun getNomisUserById(id: UUID, serviceOrigin: Cas2ServiceOrigin) = cas2UserRepository.findByIdAndServiceOrigin(id, serviceOrigin) ?: throw NotFoundProblem(id, "NomisUser")

  fun getUserByStaffId(staffId: Long, serviceOrigin: Cas2ServiceOrigin): Cas2UserEntity {
    val userDetails = cas2UserRepository.findByNomisStaffIdAndServiceOrigin(staffId, serviceOrigin)
    if (userDetails != null) {
      return userDetails
    }
    val userStaffInformation = getUserStaffInformation(staffId)
    val normalisedUsername = userStaffInformation.generalAccount.username.uppercase()
    val userDetail = getUserDetail(username = normalisedUsername)
    return ensureUserExists(username = normalisedUsername, userDetail, serviceOrigin)
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

  private fun ensureUserExists(
    username: String,
    nomisUserDetails: NomisUserDetail,
    serviceOrigin: Cas2ServiceOrigin,
  ): Cas2UserEntity {
    return cas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.NOMIS, serviceOrigin) ?: try {
      cas2UserRepository.save(
        Cas2UserEntity(
          id = UUID.randomUUID(),
          name = "${nomisUserDetails.firstName} ${nomisUserDetails.lastName}",
          username = username,
          nomisStaffId = nomisUserDetails.staffId,
          nomisAccountType = nomisUserDetails.accountType,
          email = nomisUserDetails.primaryEmail,
          isEnabled = nomisUserDetails.enabled,
          isActive = nomisUserDetails.active,
          activeNomisCaseloadId = nomisUserDetails.activeCaseloadId,
          userType = Cas2UserType.NOMIS,
          createdAt = OffsetDateTime.now(),
          serviceOrigin = serviceOrigin,
        ),
      )
    } catch (ex: DataIntegrityViolationException) {
      return cas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.NOMIS, serviceOrigin)
        ?: throw IllegalStateException("User creation failed and username $username not found", ex)
    }
  }

  private fun getCas2UserForUsername(username: String, jwt: String, userType: Cas2UserType, serviceOrigin: Cas2ServiceOrigin): Cas2UserEntity {
    val normalisedUsername = username.uppercase()

    val userEntity = when (userType) {
      Cas2UserType.NOMIS -> getCas2UserEntityForNomisUser(normalisedUsername, jwt, serviceOrigin)
      Cas2UserType.DELIUS -> getCas2UserEntityForDeliusUser(normalisedUsername, serviceOrigin)
      Cas2UserType.EXTERNAL -> getCas2UserEntityForExternalUser(normalisedUsername, jwt, serviceOrigin)
    }

    return userEntity
  }

  private fun getExistingCas2User(
    username: String,
    userType: Cas2UserType,
    serviceOrigin: Cas2ServiceOrigin,
  ): Cas2UserEntity? = cas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, userType, serviceOrigin)

  private fun getCas2UserEntityForNomisUser(username: String, jwt: String, serviceOrigin: Cas2ServiceOrigin): Cas2UserEntity {
    val nomisUserDetails: NomisUserDetail = when (
      val nomisUserDetailResponse = nomisUserRolesForRequesterApiClient.getUserDetailsForMe(jwt)
    ) {
      is ClientResult.Success -> nomisUserDetailResponse.body
      is ClientResult.Failure -> nomisUserDetailResponse.throwException()
    }

    val normalisedUsername = username.uppercase()

    val existingUser = getExistingCas2User(normalisedUsername, Cas2UserType.NOMIS, serviceOrigin)
    if (existingUser != null) {
      if (
        existingUser.email != nomisUserDetails.primaryEmail ||
        existingUser.activeNomisCaseloadId != nomisUserDetails.activeCaseloadId
      ) {
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
        username = normalisedUsername,
        nomisStaffId = nomisUserDetails.staffId,
        activeNomisCaseloadId = nomisUserDetails.activeCaseloadId,
        userType = Cas2UserType.NOMIS,
        email = nomisUserDetails.primaryEmail,
        isEnabled = nomisUserDetails.enabled,
        isActive = nomisUserDetails.active,
        deliusTeamCodes = null,
        deliusStaffCode = null,
        createdAt = OffsetDateTime.now(),
        serviceOrigin = serviceOrigin,
        nomisAccountType = nomisUserDetails.accountType,
      ),
    )
  }

  private fun getCas2UserEntityForDeliusUser(username: String, serviceOrigin: Cas2ServiceOrigin): Cas2UserEntity {
    val deliusUser: StaffDetail =
      when (val staffUserDetailsResponse = apDeliusContextApiClient.getStaffDetail(username)) {
        is ClientResult.Success<*> -> staffUserDetailsResponse.body as StaffDetail
        is ClientResult.Failure<*> -> staffUserDetailsResponse.throwException()
      }

    val existingUser = getExistingCas2User(username, Cas2UserType.DELIUS, serviceOrigin)
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
        serviceOrigin = serviceOrigin,
        createdAt = OffsetDateTime.now(),
      ),
    )
  }

  private fun getCas2UserEntityForExternalUser(username: String, jwt: String, serviceOrigin: Cas2ServiceOrigin): Cas2UserEntity {
    val existingUser = getExistingCas2User(username, Cas2UserType.EXTERNAL, serviceOrigin = serviceOrigin)
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
        createdAt = OffsetDateTime.now(),
        serviceOrigin = serviceOrigin,
        externalType = "NACRO",
      ),
    )
  }
}
