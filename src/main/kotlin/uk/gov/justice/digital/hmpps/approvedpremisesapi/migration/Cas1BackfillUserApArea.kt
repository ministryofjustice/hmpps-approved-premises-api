package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserMappingService

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
    log.info("Update ap area for user ${user.id}")
    try {
      val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
      val staffDetails = when (staffDetailsResult) {
        is ClientResult.Success -> staffDetailsResult.body
        is ClientResult.Failure -> staffDetailsResult.throwException()
      }

      val apArea = cas1UserMappingService.determineApArea(
        usersProbationRegion = user.probationRegion,
        deliusUser = staffDetails,
      )

      log.info("Updating user ${user.id} AP Area to ${apArea.name}")
      userRepository.updateApArea(user.id, apArea)
    } catch (exception: Exception) {
      log.error("Unable to find ap area for user ${user.id}", exception)
    }
  }
}
