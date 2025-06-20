package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import jakarta.transaction.Transactional
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesForRequesterApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisStaffInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import java.util.UUID

@Service
class NomisUserService(
  private val httpAuthService: HttpAuthService,
  private val nomisUserRolesApiClient: NomisUserRolesApiClient,
  private val nomisUserRolesForRequesterApiClient: NomisUserRolesForRequesterApiClient,
  private val userRepository: NomisUserRepository,
) {
  fun getUserForRequest(): NomisUserEntity {
    val authenticatedPrincipal = httpAuthService.getNomisPrincipalOrThrow()
    val jwt = authenticatedPrincipal.token.tokenValue
    val username = authenticatedPrincipal.name

    return getUserForUsername(username, jwt)
  }

  fun getNomisUserById(id: UUID) = userRepository.findById(id).orElseThrow { NotFoundProblem(id, "NomisUser") }

  fun getUserByStaffId(staffId: Long): NomisUserEntity {
    val userDetails = userRepository.findByNomisStaffId(staffId)
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
  fun getUserForUsername(username: String, jwt: String): NomisUserEntity {
    val nomisUserDetails = when (
      val nomisUserDetailResponse = nomisUserRolesForRequesterApiClient.getUserDetailsForMe(jwt)
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
    return ensureUserExists(username = normalisedUsername, nomisUserDetails)
  }

  private fun ensureUserExists(
    username: String,
    nomisUserDetails: NomisUserDetail,
  ): NomisUserEntity {
    return userRepository.findByNomisUsername(username) ?: try {
      userRepository.save(
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
      return userRepository.findByNomisUsername(username)
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
}
