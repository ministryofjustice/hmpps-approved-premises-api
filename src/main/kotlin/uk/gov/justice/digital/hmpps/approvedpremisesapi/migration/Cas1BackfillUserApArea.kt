package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserMappingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.getTeamCodes

class Cas1BackfillUserApArea(
  private val userRepository: UserRepository,
  private val cas1UserMappingService: Cas1UserMappingService,
  private val communityApiClient: CommunityApiClient,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber")
  override fun process() {
    userRepository.findAll().forEach { user ->
      transactionTemplate.executeWithoutResult {
        updateUser(user)
      }
      Thread.sleep(50)
    }

    log.info("ap area update complete")
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun updateUser(user: UserEntity) {
    log.info("Update ap area and teams for user ${user.id}")
    try {
      val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)

      val apAreaAndTeamCodes = when (staffDetailsResult) {
        is ClientResult.Success -> {
          val staffDetails = staffDetailsResult.body
          val apArea = cas1UserMappingService.determineApArea(
            usersProbationRegion = user.probationRegion,
            deliusUser = staffDetails,
          )

          AreaAndTeamCodes(apArea, staffDetails.getTeamCodes())
        }
        is ClientResult.Failure.StatusCode -> {
          if (staffDetailsResult.status == HttpStatus.NOT_FOUND) {
            log.warn("Could not find staff details for ${user.id}, will fall back to probationRegion.apArea")
            AreaAndTeamCodes(user.probationRegion.apArea, emptyList())
          } else {
            staffDetailsResult.throwException()
          }
        }
        is ClientResult.Failure -> {
          staffDetailsResult.throwException()
        }
      }

      val apArea = apAreaAndTeamCodes.apArea
      val teamCodes = apAreaAndTeamCodes.teamCodes

      log.info("Updating user ${user.id} AP Area to ${apArea.name} and team codes to $teamCodes")

      val userToUpdate = userRepository.findByIdOrNull(user.id)!!
      userToUpdate.apArea = apArea
      userToUpdate.teamCodes = teamCodes
      userRepository.save(userToUpdate)
    } catch (exception: Exception) {
      log.error("Unable to find ap area for user ${user.id}", exception)
    }
  }
}

data class AreaAndTeamCodes(
  val apArea: ApAreaEntity,
  val teamCodes: List<String>,
)
