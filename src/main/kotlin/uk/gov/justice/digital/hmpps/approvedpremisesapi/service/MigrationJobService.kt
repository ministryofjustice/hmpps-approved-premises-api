package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.PopulateNomsNumbersOnApplicationsJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.PopulateNomsNumbersOnBookingsJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateAllUsersFromCommunityApiJob

@Service
class MigrationJobService(
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val migrationLogger: MigrationLogger
) {
  @Async
  fun runMigrationJobAsync(migrationJobType: MigrationJobType) = runMigrationJob(migrationJobType)

  fun runMigrationJob(migrationJobType: MigrationJobType) {
    migrationLogger.info("Starting migration job request: $migrationJobType")

    try {
      val job: MigrationJob = when (migrationJobType) {
        MigrationJobType.updateAllUsersFromCommunityApi -> UpdateAllUsersFromCommunityApiJob(
          applicationContext.getBean(UserRepository::class.java),
          applicationContext.getBean(UserService::class.java)
        )
        MigrationJobType.populateNomsNumbersBookings -> PopulateNomsNumbersOnBookingsJob(
          applicationContext.getBean(BookingRepository::class.java),
          applicationContext.getBean(CommunityApiClient::class.java)
        )
        MigrationJobType.populateNomsNumberApplications -> PopulateNomsNumbersOnApplicationsJob(
          applicationContext.getBean(ApplicationRepository::class.java),
          applicationContext.getBean(CommunityApiClient::class.java)
        )
      }

      transactionTemplate.executeWithoutResult { job.process() }

      migrationLogger.info("Finished migration job: $migrationJobType")
    } catch (exception: Exception) {
      migrationLogger.error("Unable to complete Migration Job", exception)
    }
  }
}
