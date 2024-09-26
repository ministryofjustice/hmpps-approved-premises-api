package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class UpdateUsersPduJob(
  private val userRepository: UserRepository,
  private val userService: UserService,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  override fun process() {
    val activeUsers = userRepository.findActiveUsers()
    migrationLogger.info("Have ${activeUsers.size} users to update")
    activeUsers.forEach {
      migrationLogger.info("Updating user PDU. User id ${it.id}")
      try {
        userService.updateUserPduById(it.id)
      } catch (exception: Exception) {
        migrationLogger.error("Unable to update user PDU. User id ${it.id}", exception)
      }
      Thread.sleep(50)
    }
  }
}
