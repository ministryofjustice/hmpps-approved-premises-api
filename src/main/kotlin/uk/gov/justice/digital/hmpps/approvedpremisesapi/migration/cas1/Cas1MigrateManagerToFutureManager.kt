package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class Cas1MigrateManagerToFutureManager(
  private val userService: UserService,
  private val userRepository: UserRepository,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun process() {
    val users = userRepository.findUsersWithRole(UserRole.CAS1_MANAGER)

    log.info("Have found ${users.size} users with the role CAS1_MANAGER")

    users.forEach { user ->
      userService.removeRoleFromUser(user, UserRole.CAS1_MANAGER)
      userService.addRoleToUser(user, UserRole.CAS1_FUTURE_MANAGER)

      log.info(
        """
Adding FUTURE_MANAGER role to ${user.deliusUsername}
-> ${user.deliusUsername} now has roles: ${user.roles.map { it.role }.joinToString(",")}
        """.trimIndent(),
      )
    }
  }
}
