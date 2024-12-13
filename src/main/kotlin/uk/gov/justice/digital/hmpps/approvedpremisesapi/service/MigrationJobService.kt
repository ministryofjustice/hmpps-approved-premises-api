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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1UpdateApplicationLicenceExpiryDateJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.UpdateLicenceExpiryDateRepository
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
        MigrationJobType.allUsersFromCommunityApi -> UpdateAllUsersFromDeliusJob(
          getBean(UserRepository::class),
          getBean(UserService::class),
        )

        MigrationJobType.sentenceTypeAndSituation -> UpdateSentenceTypeAndSituationJob(
          getBean(UpdateSentenceTypeAndSituationRepository::class),
        )

        MigrationJobType.bookingStatus -> BookingStatusMigrationJob(
          getBean(BookingRepository::class),
          getBean(EntityManager::class),
        )

        MigrationJobType.taskDueDates -> Cas1TaskDueMigrationJob(
          getBean(AssessmentRepository::class),
          getBean(PlacementApplicationRepository::class),
          getBean(PlacementRequestRepository::class),
          getBean(EntityManager::class),
          getBean(TaskDeadlineService::class),
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
        )

        MigrationJobType.cas2NotesWithAssessments -> Cas2NoteMigrationJob(
          getBean(Cas2ApplicationNoteRepository::class),
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
        )

        MigrationJobType.cas1BackfillUserApArea -> Cas1BackfillUserApArea(
          getBean(UserRepository::class),
          getBean(UserService::class),
          getBean(ApDeliusContextApiClient::class),
          transactionTemplate,
        )

        MigrationJobType.cas3ApplicationOffenderName -> Cas3UpdateApplicationOffenderNameJob(
          getBean(ApplicationRepository::class),
          getBean(OffenderService::class),
          getBean(EntityManager::class),
          getBean(MigrationLogger::class),
        )

        MigrationJobType.cas3DomainEventTypeForPersonDepartedUpdated -> Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob(
          getBean(DomainEventRepository::class),
          getBean(ObjectMapper::class),
          getBean(MigrationLogger::class),
        )

        MigrationJobType.cas1ApplicationsLicenceExpiryDate -> Cas1UpdateApplicationLicenceExpiryDateJob(
          getBean(UpdateLicenceExpiryDateRepository::class),
          getBean(MigrationLogger::class),
        )
      }

      if (job.shouldRunInTransaction) {
        transactionTemplate.executeWithoutResult { job.process(pageSize) }
      } else {
        job.process(pageSize)
      }

      migrationLogger.info("Finished migration job: $migrationJobType")
    } catch (exception: Exception) {
      Sentry.captureException(exception)
      migrationLogger.error("Unable to complete Migration Job", exception)
    }
  }

  private fun <T : Any> getBean(clazz: KClass<T>) = applicationContext.getBean(clazz.java)
}
