package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateAllUsersFromCommunityApiJob

@Service
class MigrationJobService(
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val migrationLogger: MigrationLogger,
) {
  @Async
  fun runMigrationJobAsync(migrationJobType: MigrationJobType) = runMigrationJob(migrationJobType)

  fun runMigrationJob(migrationJobType: MigrationJobType) {
    migrationLogger.info("Starting migration job request: $migrationJobType")

    try {
      val job: MigrationJob = when (migrationJobType) {
        MigrationJobType.updateAllUsersFromCommunityApi -> UpdateAllUsersFromCommunityApiJob(
          applicationContext.getBean(UserRepository::class.java),
          applicationContext.getBean(UserService::class.java),
        )
      }

      if (job.shouldRunInTransaction) {
        transactionTemplate.executeWithoutResult { job.process() }
      } else {
        job.process()
      }

      migrationLogger.info("Finished migration job: $migrationJobType")
    } catch (exception: Exception) {
      migrationLogger.error("Unable to complete Migration Job", exception)
    }
  }
}
