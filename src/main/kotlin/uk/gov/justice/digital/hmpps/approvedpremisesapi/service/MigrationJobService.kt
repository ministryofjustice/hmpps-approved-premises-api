package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.sentry.Sentry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.BookingStatusMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.NoticeTypeMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.NoticeTypeMigrationJobApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateAllUsersFromCommunityApiJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateSentenceTypeAndSituationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateSentenceTypeAndSituationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateUsersPduFromCommunityApiJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.ApAreaMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.ApAreaMigrationJobApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1BackfillUserApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1FixPlacementApplicationLinksJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1LostBedsToOutOfServiceBedsMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1OutOfServiceBedReasonMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1ReasonForShortNoticeMetadataMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1UserDetailsMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.LostBedMigrationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.TaskDueMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2AssessmentMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2NoteMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2StatusUpdateMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateApplicationOffenderNameJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob
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

  @SuppressWarnings("CyclomaticComplexMethod")
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

        MigrationJobType.usersPduFromCommunityApi -> UpdateUsersPduFromCommunityApiJob(
          applicationContext.getBean(UserRepository::class.java),
          applicationContext.getBean(UserService::class.java),
          applicationContext.getBean(MigrationLogger::class.java),
        )

        MigrationJobType.cas2ApplicationsWithAssessments -> Cas2AssessmentMigrationJob(
          applicationContext.getBean(Cas2AssessmentRepository::class.java),
          applicationContext.getBean(Cas2ApplicationRepository::class.java),
          transactionTemplate,
        )

        MigrationJobType.cas2StatusUpdatesWithAssessments -> Cas2StatusUpdateMigrationJob(
          applicationContext.getBean(Cas2StatusUpdateRepository::class.java),
          transactionTemplate,
          pageSize,
        )

        MigrationJobType.cas2NotesWithAssessments -> Cas2NoteMigrationJob(
          applicationContext.getBean(Cas2ApplicationNoteRepository::class.java),
          transactionTemplate,
          pageSize,
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
          applicationContext.getBean(EntityManager::class.java),
          pageSize,
        )

        MigrationJobType.cas1BackfillUserApArea -> Cas1BackfillUserApArea(
          applicationContext.getBean(UserRepository::class.java),
          applicationContext.getBean(UserService::class.java),
          applicationContext.getBean(CommunityApiClient::class.java),
          transactionTemplate,
        )

        MigrationJobType.cas1OutOfServiceBedReasons -> Cas1OutOfServiceBedReasonMigrationJob(
          applicationContext.getBean(LostBedReasonRepository::class.java),
          applicationContext.getBean(Cas1OutOfServiceBedReasonRepository::class.java),
        )

        MigrationJobType.cas1LostBedsToOutOfServiceBeds -> Cas1LostBedsToOutOfServiceBedsMigrationJob(
          applicationContext.getBean(LostBedMigrationRepository::class.java),
          applicationContext.getBean(Cas1OutOfServiceBedRepository::class.java),
          applicationContext.getBean(Cas1OutOfServiceBedCancellationRepository::class.java),
          applicationContext.getBean(Cas1OutOfServiceBedReasonRepository::class.java),
          applicationContext.getBean(Cas1OutOfServiceBedRevisionRepository::class.java),
          transactionTemplate,
          applicationContext.getBean(MigrationLogger::class.java),
        )

        MigrationJobType.cas3ApplicationOffenderName -> Cas3UpdateApplicationOffenderNameJob(
          applicationContext.getBean(ApplicationRepository::class.java),
          applicationContext.getBean(OffenderService::class.java),
          applicationContext.getBean(EntityManager::class.java),
          pageSize,
          applicationContext.getBean(MigrationLogger::class.java),
        )

        MigrationJobType.cas1PopulateAppReasonForShortNoticeMetadata -> Cas1ReasonForShortNoticeMetadataMigrationJob(
          applicationContext.getBean(ApplicationRepository::class.java),
          applicationContext.getBean(DomainEventRepository::class.java),
          applicationContext.getBean(TransactionTemplate::class.java),
          applicationContext.getBean(JdbcTemplate::class.java),
        )

        MigrationJobType.cas3DomainEventTypeForPersonDepartedUpdated -> Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob(
          applicationContext.getBean(DomainEventRepository::class.java),
          applicationContext.getBean(ObjectMapper::class.java),
          applicationContext.getBean(MigrationLogger::class.java),
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
