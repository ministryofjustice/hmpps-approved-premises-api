package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import io.sentry.Sentry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.ApAreaMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.ApAreaMigrationJobApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.BookingStatusMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.Cas1FixPlacementApplicationLinksJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.Cas1UserDetailsMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.Cas2AssessmentMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.NoticeTypeMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.NoticeTypeMigrationJobApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.TaskDueMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateAllUsersFromCommunityApiJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateSentenceTypeAndSituationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateSentenceTypeAndSituationRepository
import javax.persistence.EntityManager

@Service
class MigrationJobService(
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val migrationLogger: MigrationLogger,
  @Value("\${migration-job.throttle-enabled}") private val throttle: Boolean,
) {
  @Async
  fun runMigrationJobAsync(migrationJobType: MigrationJobType) = runMigrationJob(migrationJobType, 50)

  fun runMigrationJob(migrationJobType: MigrationJobType, pageSize: Int = 10) {
    migrationLogger.info("Starting migration job request: $migrationJobType")

    try {
      val job: MigrationJob = when (migrationJobType) {
        MigrationJobType.allUsersFromCommunityApi -> UpdateAllUsersFromCommunityApiJob(
          applicationContext.getBean(UserRepository::class.java),
          applicationContext.getBean(UserService::class.java),
        )
        MigrationJobType.sentenceTypeAndSituation -> UpdateSentenceTypeAndSituationJob(
          applicationContext.getBean(UpdateSentenceTypeAndSituationRepository::class.java),
        )

        MigrationJobType.bookingStatus -> BookingStatusMigrationJob(
          applicationContext.getBean(BookingRepository::class.java),
          applicationContext.getBean(EntityManager::class.java),
          pageSize,
        )

        MigrationJobType.applicationApAreas -> ApAreaMigrationJob(
          applicationContext.getBean(ApAreaMigrationJobApplicationRepository::class.java),
          applicationContext.getBean(ApAreaRepository::class.java),
          transactionTemplate,
        )

        MigrationJobType.taskDueDates -> TaskDueMigrationJob(
          applicationContext.getBean(AssessmentRepository::class.java),
          applicationContext.getBean(PlacementApplicationRepository::class.java),
          applicationContext.getBean(PlacementRequestRepository::class.java),
          applicationContext.getBean(EntityManager::class.java),
          applicationContext.getBean(TaskDeadlineService::class.java),
          pageSize,
        )

        MigrationJobType.cas2ApplicationsWithAssessments -> Cas2AssessmentMigrationJob(
          applicationContext.getBean(Cas2AssessmentRepository::class.java),
          applicationContext.getBean(Cas2ApplicationRepository::class.java),
          transactionTemplate,
        )

        MigrationJobType.cas1UserDetails -> Cas1UserDetailsMigrationJob(
          applicationContext.getBean(ApplicationRepository::class.java),
          applicationContext.getBean(Cas1ApplicationUserDetailsRepository::class.java),
          applicationContext.getBean(EntityManager::class.java),
          pageSize,
          transactionTemplate,
        )

        MigrationJobType.cas1FixPlacementAppLinks -> Cas1FixPlacementApplicationLinksJob(
          applicationContext.getBean(PlacementApplicationRepository::class.java),
          applicationContext.getBean(ApplicationRepository::class.java),
          applicationContext.getBean(PlacementRequestRepository::class.java),
          applicationContext.getBean(EntityManager::class.java),
          transactionTemplate,
        )

        MigrationJobType.cas1NoticeTypes -> NoticeTypeMigrationJob(
          applicationContext.getBean(NoticeTypeMigrationJobApplicationRepository::class.java),
          pageSize,
        )
      }

      if (job.shouldRunInTransaction) {
        transactionTemplate.executeWithoutResult { job.process() }
      } else {
        job.process()
      }

      migrationLogger.info("Finished migration job: $migrationJobType")
    } catch (exception: Exception) {
      Sentry.captureException(exception)
      migrationLogger.error("Unable to complete Migration Job", exception)
    }
  }
}
