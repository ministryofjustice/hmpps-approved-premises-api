package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesForRequesterApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nomisuserroles.NomisUserDetail
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2NomisUserService(
  private val nomisUserRolesForRequesterApiClient: NomisUserRolesForRequesterApiClient,
  private val cas2UserRepository: Cas2UserRepository,
) {

  @Transactional(value = Transactional.TxType.REQUIRES_NEW)
  fun getCas2UserEntityForNomisUser(username: String, jwt: String, serviceOrigin: Cas2ServiceOrigin): Cas2UserEntity {
    val nomisUserDetails: NomisUserDetail = when (
      val nomisUserDetailResponse = nomisUserRolesForRequesterApiClient.getUserDetailsForMe(jwt)
    ) {
      is ClientResult.Success -> nomisUserDetailResponse.body
      is ClientResult.Failure -> nomisUserDetailResponse.throwException()
    }

    val existingUser = cas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.NOMIS, serviceOrigin)
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
        username = username,
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
}
