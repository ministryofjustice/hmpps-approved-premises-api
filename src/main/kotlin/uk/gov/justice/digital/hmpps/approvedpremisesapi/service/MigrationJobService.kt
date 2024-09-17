package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.sentry.Sentry
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateUsersPduJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.ApAreaMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.ApAreaMigrationJobApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1BackfillUserApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1FixPlacementApplicationLinksJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1LostBedsToOutOfServiceBedsMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1MigrateManagerToFutureManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1OutOfServiceBedReasonMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1ReasonForShortNoticeMetadataMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1TaskDueMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1TruncateOosbMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1UserDetailsMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.LostBedMigrationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2AssessmentMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2NoteMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2StatusUpdateMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateApplicationOffenderNameJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import kotlin.reflect.KClass

@Service
class MigrationJobService(
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val migrationLogger: MigrationLogger,
) {
  @Async
  fun runMigrationJobAsync(migrationJobType: MigrationJobType) = runMigrationJob(migrationJobType, 50)

  @SuppressWarnings("CyclomaticComplexMethod")
  fun runMigrationJob(migrationJobType: MigrationJobType, pageSize: Int = 10) {
    migrationLogger.info("Starting migration job request: $migrationJobType")

    try {
      val job: MigrationJob = when (migrationJobType) {
        MigrationJobType.allUsersFromCommunityApi -> UpdateAllUsersFromCommunityApiJob(
          getBean(UserRepository::class),
          getBean(UserService::class),
        )

        MigrationJobType.sentenceTypeAndSituation -> UpdateSentenceTypeAndSituationJob(
          getBean(UpdateSentenceTypeAndSituationRepository::class),
        )

        MigrationJobType.bookingStatus -> BookingStatusMigrationJob(
          getBean(BookingRepository::class),
          getBean(EntityManager::class),
          pageSize,
        )

        MigrationJobType.applicationApAreas -> ApAreaMigrationJob(
          getBean(ApAreaMigrationJobApplicationRepository::class),
          getBean(ApAreaRepository::class),
          transactionTemplate,
        )

        MigrationJobType.taskDueDates -> Cas1TaskDueMigrationJob(
          getBean(AssessmentRepository::class),
          getBean(PlacementApplicationRepository::class),
          getBean(PlacementRequestRepository::class),
          getBean(EntityManager::class),
          getBean(TaskDeadlineService::class),
          pageSize,
        )

        MigrationJobType.usersPduByApi -> UpdateUsersPduJob(
          getBean(UserRepository::class),
          getBean(UserService::class),
          getBean(MigrationLogger::class),
        )

        MigrationJobType.cas2ApplicationsWithAssessments -> Cas2AssessmentMigrationJob(
          getBean(Cas2AssessmentRepository::class),
          getBean(Cas2ApplicationRepository::class),
          transactionTemplate,
        )

        MigrationJobType.cas2StatusUpdatesWithAssessments -> Cas2StatusUpdateMigrationJob(
          getBean(Cas2StatusUpdateRepository::class),
          transactionTemplate,
          pageSize,
        )

        MigrationJobType.cas2NotesWithAssessments -> Cas2NoteMigrationJob(
          getBean(Cas2ApplicationNoteRepository::class),
          transactionTemplate,
          pageSize,
        )

        MigrationJobType.cas1UserDetails -> Cas1UserDetailsMigrationJob(
          getBean(ApplicationRepository::class),
          getBean(Cas1ApplicationUserDetailsRepository::class),
          getBean(EntityManager::class),
          pageSize,
          transactionTemplate,
        )

        MigrationJobType.cas1FixPlacementAppLinks -> Cas1FixPlacementApplicationLinksJob(
          getBean(PlacementApplicationRepository::class),
          getBean(ApplicationRepository::class),
          getBean(PlacementRequestRepository::class),
          getBean(EntityManager::class),
          transactionTemplate,
        )

        MigrationJobType.cas1NoticeTypes -> NoticeTypeMigrationJob(
          getBean(NoticeTypeMigrationJobApplicationRepository::class),
          getBean(EntityManager::class),
          pageSize,
        )

        MigrationJobType.cas1BackfillUserApArea -> Cas1BackfillUserApArea(
          getBean(UserRepository::class),
          getBean(UserService::class),
          getBean(ApDeliusContextApiClient::class),
          transactionTemplate,
        )

        MigrationJobType.cas1OutOfServiceBedReasons -> Cas1OutOfServiceBedReasonMigrationJob(
          getBean(LostBedReasonRepository::class),
          getBean(Cas1OutOfServiceBedReasonRepository::class),
        )

        MigrationJobType.cas1LostBedsToOutOfServiceBeds -> Cas1LostBedsToOutOfServiceBedsMigrationJob(
          getBean(LostBedMigrationRepository::class),
          getBean(Cas1OutOfServiceBedRepository::class),
          getBean(Cas1OutOfServiceBedCancellationRepository::class),
          getBean(Cas1OutOfServiceBedReasonRepository::class),
          getBean(Cas1OutOfServiceBedRevisionRepository::class),
          transactionTemplate,
          getBean(MigrationLogger::class),
        )

        MigrationJobType.cas3ApplicationOffenderName -> Cas3UpdateApplicationOffenderNameJob(
          getBean(ApplicationRepository::class),
          getBean(OffenderService::class),
          getBean(EntityManager::class),
          pageSize,
          getBean(MigrationLogger::class),
        )

        MigrationJobType.cas1PopulateAppReasonForShortNoticeMetadata -> Cas1ReasonForShortNoticeMetadataMigrationJob(
          getBean(ApplicationRepository::class),
          getBean(DomainEventRepository::class),
          getBean(TransactionTemplate::class),
          getBean(JdbcTemplate::class),
        )

        MigrationJobType.cas3DomainEventTypeForPersonDepartedUpdated -> Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob(
          getBean(DomainEventRepository::class),
          getBean(ObjectMapper::class),
          getBean(MigrationLogger::class),
        )

        MigrationJobType.cas1ManagerToFutureManager -> Cas1MigrateManagerToFutureManager(
          getBean(UserService::class),
          getBean(UserRepository::class),
        )

        MigrationJobType.cas1TruncateOosbForBedsWithEndDate -> Cas1TruncateOosbMigrationJob(
          getBean(Cas1OutOfServiceBedRepository::class),
          getBean(Cas1OutOfServiceBedService::class),
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

  private fun <T : Any> getBean(clazz: KClass<T>) = applicationContext.getBean(clazz.java)
}
