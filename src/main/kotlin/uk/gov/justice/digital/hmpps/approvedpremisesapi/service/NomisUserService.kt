package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.nomisuserroles.NomisUserDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@Service
class NomisUserService(
  private val currentRequest: HttpServletRequest,
  private val httpAuthService: HttpAuthService,
  private val offenderService: OffenderService,
  private val nomisUserRolesApiClient: NomisUserRolesApiClient,
  private val userRepository: NomisUserRepository,
  private val userTransformer: UserTransformer,
) {
  private val log = LoggerFactory.getLogger(this::class.java)


  fun getUserForRequest(): NomisUserEntity {
    val authenticatedPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = authenticatedPrincipal.name

    return getUserForUsername(username)
  }

  fun getUserForUsername(username: String): NomisUserEntity {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByNomisUsername(normalisedUsername)
    if (existingUser != null) return existingUser

    val nomisUserDetailResponse = nomisUserRolesApiClient.getUserDetails(normalisedUsername)

    val nomisUserDetails = when (nomisUserDetailResponse) {
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
        enabled = nomisUserDetails.enabled,
        active = nomisUserDetails.active,
        applications = mutableListOf()
      ),
    )
  }
}
