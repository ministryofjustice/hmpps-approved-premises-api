package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import io.sentry.Sentry
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration.Cas2AssessmentMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration.Cas2NoteMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration.Cas2StatusUpdateMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1ArsonSuitableToArsonOffencesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1BackfillOfflineApplicationName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1BackfillUserApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1IsArsonSuitableBackfillJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1TaskDueMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1UpdateApplicationLicenceExpiryDateJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1UpdateApprovedPremisesApplicationWithOffenderJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1UpdateAssessmentReportPropertiesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1UpdateRoomCodesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3MigrateNewBedspaceModelDataJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateApplicationOffenderNameJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateBedSpaceStartDateJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateBookingOffenderNameJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdatePremisesStartDateJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3VoidBedspaceCancellationJob
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
        MigrationJobType.updateCas3BedspaceStartDate -> getBean(Cas3UpdateBedSpaceStartDateJob::class)
        MigrationJobType.updateCas3PremisesStartDate -> getBean(Cas3UpdatePremisesStartDateJob::class)
        MigrationJobType.updateCas3DomainEventTypeForPersonDepartedUpdated -> getBean(Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob::class)
        MigrationJobType.updateCas1ApplicationsLicenceExpiryDate -> getBean(Cas1UpdateApplicationLicenceExpiryDateJob::class)
        MigrationJobType.updateCas1BackfillOfflineApplicationName -> getBean(Cas1BackfillOfflineApplicationName::class)
        MigrationJobType.updateCas1ArsonSuitableToArsonOffences -> getBean(Cas1ArsonSuitableToArsonOffencesJob::class)
        MigrationJobType.updateCas1BackfillArsonSuitable -> getBean(Cas1IsArsonSuitableBackfillJob::class)
        MigrationJobType.updateCas1ApprovedPremisesAssessmentReportProperties -> getBean(Cas1UpdateAssessmentReportPropertiesJob::class)
        MigrationJobType.cas1UpdateRoomCodes -> getBean(Cas1UpdateRoomCodesJob::class)
        MigrationJobType.updateCas1ApplicationsWithOffender -> getBean(Cas1UpdateApprovedPremisesApplicationWithOffenderJob::class)
        MigrationJobType.updateCas3BedspaceModelData -> getBean(Cas3MigrateNewBedspaceModelDataJob::class)
        MigrationJobType.updateCas3VoidBedspaceCancellationData -> getBean(Cas3VoidBedspaceCancellationJob::class)
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
