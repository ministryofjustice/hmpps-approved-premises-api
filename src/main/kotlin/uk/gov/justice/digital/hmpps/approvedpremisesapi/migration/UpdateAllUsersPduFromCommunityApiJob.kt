package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class UpdateAllUsersPduFromCommunityApiJob(
  private val userRepository: UserRepository,
  private val userService: UserService,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  override fun process() {
    userRepository.findAll().forEach {
      migrationLogger.info("Updating user PDU. User id ${it.id}")
      try {
        userService.updateUserPduFromCommunityApiById(it.id)
      } catch (exception: Exception) {
        migrationLogger.error("Unable to update user PDU. User id ${it.id}", exception)
      }
      Thread.sleep(500)
    }
  }
}
