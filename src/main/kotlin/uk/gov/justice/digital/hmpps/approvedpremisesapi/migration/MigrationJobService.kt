package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import io.sentry.Sentry
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1BackfillApplicationDuration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1BackfillAutomaticPlacementApplicationsJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1BackfillKeyWorkerUserAssignmentsJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1BackfillUserApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1CapacityPerformanceTestJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1TaskDueMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1UpdateApprovedPremisesApplicationWithOffenderJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1UpdateAssessmentReportPropertiesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.Cas1UpdateRoomCodesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.UpdateSentenceTypeAndSituationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration.Cas2AssessmentMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration.Cas2NoteMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration.Cas2StatusUpdateMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.BookingStatusMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3MigrateNewBedspaceModelDataJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3UpdateApplicationOffenderNameJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3UpdateArchivedPremisesEndDateJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3UpdateBedspaceCreatedDateJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3UpdateBookingOffenderNameJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3UpdatePremisesCreatedDateJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3UpdatePremisesStartDateJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3VoidBedspaceJob
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
        MigrationJobType.updateAllUsersFromCommunityApi -> getBean(UpdateAllUsersFromDeliusJob::class)
        MigrationJobType.updateSentenceTypeAndSituation -> getBean(UpdateSentenceTypeAndSituationJob::class)
        MigrationJobType.updateBookingStatus -> getBean(BookingStatusMigrationJob::class)
        MigrationJobType.updateTaskDueDates -> getBean(Cas1TaskDueMigrationJob::class)
        MigrationJobType.updateUsersPduByApi -> getBean(UpdateUsersPduJob::class)
        MigrationJobType.updateCas2ApplicationsWithAssessments -> getBean(Cas2AssessmentMigrationJob::class)
        MigrationJobType.updateCas2StatusUpdatesWithAssessments -> getBean(Cas2StatusUpdateMigrationJob::class)
        MigrationJobType.updateCas2NotesWithAssessments -> getBean(Cas2NoteMigrationJob::class)
        MigrationJobType.updateCas1BackfillUserApArea -> getBean(Cas1BackfillUserApArea::class)
        MigrationJobType.updateCas3ApplicationOffenderName -> getBean(Cas3UpdateApplicationOffenderNameJob::class)
        MigrationJobType.updateCas3BookingOffenderName -> getBean(Cas3UpdateBookingOffenderNameJob::class)
        MigrationJobType.updateCas3PremisesStartDate -> getBean(Cas3UpdatePremisesStartDateJob::class)
        MigrationJobType.updateCas3ArchivedPremisesEndDate -> getBean(Cas3UpdateArchivedPremisesEndDateJob::class)
        MigrationJobType.updateCas3PremisesCreatedAt -> getBean(Cas3UpdatePremisesCreatedDateJob::class)
        MigrationJobType.updateCas3BedspaceCreatedAt -> getBean(Cas3UpdateBedspaceCreatedDateJob::class)
        MigrationJobType.updateCas3DomainEventTypeForPersonDepartedUpdated -> getBean(Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob::class)
        MigrationJobType.updateCas1ApprovedPremisesAssessmentReportProperties -> getBean(Cas1UpdateAssessmentReportPropertiesJob::class)
        MigrationJobType.cas1UpdateRoomCodes -> getBean(Cas1UpdateRoomCodesJob::class)
        MigrationJobType.updateCas1ApplicationsWithOffender -> getBean(Cas1UpdateApprovedPremisesApplicationWithOffenderJob::class)
        MigrationJobType.updateCas3BedspaceModelData -> getBean(Cas3MigrateNewBedspaceModelDataJob::class)
        MigrationJobType.updateCas3VoidBedspaceData -> getBean(Cas3VoidBedspaceJob::class)
        MigrationJobType.cas1BackfillApplicationDuration -> getBean(Cas1BackfillApplicationDuration::class)
        MigrationJobType.cas1BackfillAutomaticPlacementApplications -> getBean(Cas1BackfillAutomaticPlacementApplicationsJob::class)
        MigrationJobType.cas1BackfillKeyWorkerUserAssignments -> getBean(Cas1BackfillKeyWorkerUserAssignmentsJob::class)
        MigrationJobType.cas1CapacityPerformanceTest -> getBean(Cas1CapacityPerformanceTestJob::class)
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
