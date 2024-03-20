package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserMappingService

class Cas1ValidateApAreaUserMapping(
  private val userRepository: UserRepository,
  private val userMappingService: UserMappingService,
  private val communityApiClient: CommunityApiClient,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  override fun process() {
    userRepository.findAll().forEach { user ->
      log.info("Check ap area can be determined for user ${user.id}")
      try {
        val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
        val staffDetails = when (staffDetailsResult) {
          is ClientResult.Success -> staffDetailsResult.body
          is ClientResult.Failure -> staffDetailsResult.throwException()
        }

        userMappingService.determineApArea(
          usersProbationRegion = user.probationRegion,
          deliusUser = staffDetails,
        )
      } catch (exception: Exception) {
        log.error("Unable to update user ${user.id}", exception)
      }

      Thread.sleep(100)
    }
  }
}
