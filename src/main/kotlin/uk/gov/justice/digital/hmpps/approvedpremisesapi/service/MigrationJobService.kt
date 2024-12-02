package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.sentry.Sentry
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.BookingStatusMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.NoticeTypeMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.NoticeTypeMigrationJobApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateAllUsersFromDeliusJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateSentenceTypeAndSituationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateSentenceTypeAndSituationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateUsersPduJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1BackfillUserApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1FixPlacementApplicationLinksJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1TaskDueMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2AssessmentMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2NoteMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2StatusUpdateMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateApplicationOffenderNameJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob
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
        MigrationJobType.ALL_USERS_FROM_COMMUNITY_API -> UpdateAllUsersFromDeliusJob(
          getBean(UserRepository::class),
          getBean(UserService::class),
        )

        MigrationJobType.SENTENCE_TYPE_AND_SITUATION -> UpdateSentenceTypeAndSituationJob(
          getBean(UpdateSentenceTypeAndSituationRepository::class),
        )

        MigrationJobType.BOOKING_STATUS -> BookingStatusMigrationJob(
          getBean(BookingRepository::class),
          getBean(EntityManager::class),
          pageSize,
        )

        MigrationJobType.TASK_DUE_DATES -> Cas1TaskDueMigrationJob(
          getBean(AssessmentRepository::class),
          getBean(PlacementApplicationRepository::class),
          getBean(PlacementRequestRepository::class),
          getBean(EntityManager::class),
          getBean(TaskDeadlineService::class),
          pageSize,
        )

        MigrationJobType.USERS_PDU_BY_API -> UpdateUsersPduJob(
          getBean(UserRepository::class),
          getBean(UserService::class),
          getBean(MigrationLogger::class),
        )

        MigrationJobType.CAS2_APPLICATIONS_WITH_ASSESSMENTS -> Cas2AssessmentMigrationJob(
          getBean(Cas2AssessmentRepository::class),
          getBean(Cas2ApplicationRepository::class),
          transactionTemplate,
        )

        MigrationJobType.CAS2_STATUS_UPDATES_WITH_ASSESSMENTS -> Cas2StatusUpdateMigrationJob(
          getBean(Cas2StatusUpdateRepository::class),
          transactionTemplate,
          pageSize,
        )

        MigrationJobType.CAS2_NOTES_WITH_ASSESSMENTS -> Cas2NoteMigrationJob(
          getBean(Cas2ApplicationNoteRepository::class),
          transactionTemplate,
          pageSize,
        )

        MigrationJobType.CAS1_FIX_PLACEMENT_APP_LINKS -> Cas1FixPlacementApplicationLinksJob(
          getBean(PlacementApplicationRepository::class),
          getBean(ApplicationRepository::class),
          getBean(PlacementRequestRepository::class),
          getBean(EntityManager::class),
          transactionTemplate,
        )

        MigrationJobType.CAS1_NOTICE_TYPES -> NoticeTypeMigrationJob(
          getBean(NoticeTypeMigrationJobApplicationRepository::class),
          getBean(EntityManager::class),
          pageSize,
        )

        MigrationJobType.CAS1_BACKFILL_USER_AP_AREA -> Cas1BackfillUserApArea(
          getBean(UserRepository::class),
          getBean(UserService::class),
          getBean(ApDeliusContextApiClient::class),
          transactionTemplate,
        )

        MigrationJobType.CAS3_APPLICATION_OFFENDER_NAME -> Cas3UpdateApplicationOffenderNameJob(
          getBean(ApplicationRepository::class),
          getBean(OffenderService::class),
          getBean(EntityManager::class),
          pageSize,
          getBean(MigrationLogger::class),
        )

        MigrationJobType.CAS3_DOMAIN_EVENT_TYPE_FOR_PERSON_DEPARTED_UPDATED -> Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob(
          getBean(DomainEventRepository::class),
          getBean(ObjectMapper::class),
          getBean(MigrationLogger::class),
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
