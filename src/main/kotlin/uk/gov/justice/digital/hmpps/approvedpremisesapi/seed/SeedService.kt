package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApStaffUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesBookingCancelSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesRoomsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingToSpaceBookingSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1CruManagementAreaSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1DomainEventReplaySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1DuplicateApplicationSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1FurtherInfoBugFixSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1ImportDeliusBookingDataSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1LinkedBookingToPlacementRequestSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1OutOfServiceBedSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1PlanSpacePlanningDryRunSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1RemoveAssessmentDetailsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateEventNumberSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1WithdrawPlacementRequestSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.Cas2ApplicationsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.ExternalUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.NomisUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.Cas3UsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.TemporaryAccommodationBedspaceSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.TemporaryAccommodationPremisesSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.findRootCause
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.io.path.absolutePathString
import kotlin.reflect.KClass

@Service
class SeedService(
  private val seedConfig: SeedConfig,
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val seedLogger: SeedLogger,
) {
  @Async
  fun seedDataAsync(seedFileType: SeedFileType, filename: String) = seedData(seedFileType, filename)

  fun seedData(seedFileType: SeedFileType, filename: String) = seedData(seedFileType, filename) { "${seedConfig.filePrefix}/$filename" }

  @SuppressWarnings("CyclomaticComplexMethod", "TooGenericExceptionThrown")
  fun seedData(seedFileType: SeedFileType, filename: String, resolveCsvPath: SeedJob<*>.() -> String) {
    seedLogger.info("Starting seed request: $seedFileType - $filename")

    try {
      if (filename.contains("/") || filename.contains("\\")) {
        throw RuntimeException("Filename must be just the filename of a .csv file in the /seed directory, e.g. for /seed/upload.csv, just `upload` should be supplied")
      }

      val job: SeedJob<*> = when (seedFileType) {
        SeedFileType.APPROVED_PREMISES -> ApprovedPremisesSeedJob(
          getBean(PremisesRepository::class),
          getBean(ProbationRegionRepository::class),
          getBean(LocalAuthorityAreaRepository::class),
          getBean(CharacteristicRepository::class),
        )
        SeedFileType.APPROVED_PREMISES_ROOMS -> ApprovedPremisesRoomsSeedJob(
          getBean(PremisesRepository::class),
          getBean(RoomRepository::class),
          getBean(BedRepository::class),
          getBean(CharacteristicRepository::class),
        )
        SeedFileType.USER -> AllCasUsersSeedJob(
          getBean(UserService::class),
        )
        SeedFileType.APPROVED_PREMISES_AP_STAFF_USERS -> ApStaffUsersSeedJob(
          getBean(UserService::class),
          getBean(SeedLogger::class),
        )
        SeedFileType.NOMIS_USERS -> NomisUsersSeedJob(
          getBean(NomisUserRepository::class),
        )
        SeedFileType.EXTERNAL_USERS -> ExternalUsersSeedJob(
          getBean(ExternalUserRepository::class),
        )
        SeedFileType.CAS2_APPLICATIONS -> Cas2ApplicationsSeedJob(
          getBean(Cas2ApplicationRepository::class),
          getBean(NomisUserRepository::class),
          getBean(ExternalUserRepository::class),
          getBean(Cas2StatusUpdateRepository::class),
          getBean(Cas2AssessmentRepository::class),
          getBean(JsonSchemaService::class),
          getBean(Cas2PersistedApplicationStatusFinder::class),
        )
        SeedFileType.APPROVED_PREMISES_USERS -> Cas1UsersSeedJob(
          getBean(UserService::class),
        )
        SeedFileType.TEMPORARY_ACCOMMODATION_USERS -> Cas3UsersSeedJob(
          getBean(UserService::class),
        )
        SeedFileType.CHARACTERISTICS -> CharacteristicsSeedJob(
          getBean(CharacteristicRepository::class),
        )
        SeedFileType.UPDATE_NOMS_NUMBER -> Cas1UpdateNomsNumberSeedJob(
          getBean(ApplicationRepository::class),
          getBean(ApplicationTimelineNoteService::class),
          getBean(BookingRepository::class),
        )
        SeedFileType.TEMPORARY_ACCOMMODATION_PREMISES -> TemporaryAccommodationPremisesSeedJob(
          getBean(PremisesRepository::class),
          getBean(ProbationRegionRepository::class),
          getBean(LocalAuthorityAreaRepository::class),
          getBean(ProbationDeliveryUnitRepository::class),
          getBean(CharacteristicService::class),
        )
        SeedFileType.TEMPORARY_ACCOMMODATION_BEDSPACE -> TemporaryAccommodationBedspaceSeedJob(
          getBean(PremisesRepository::class),
          getBean(CharacteristicService::class),
          getBean(RoomService::class),
        )

        SeedFileType.APPROVED_PREMISES_CANCEL_BOOKINGS -> ApprovedPremisesBookingCancelSeedJob(
          getBean(BookingService::class),
          getBean(BookingRepository::class),
        )

        SeedFileType.APPROVED_PREMISES_ASSESSMENT_MORE_INFO_BUG_FIX -> Cas1FurtherInfoBugFixSeedJob(
          getBean(AssessmentRepository::class),
        )

        SeedFileType.APPROVED_PREMISES_REDACT_ASSESSMENT_DETAILS -> Cas1RemoveAssessmentDetailsSeedJob(
          getBean(AssessmentRepository::class),
          getBean(ObjectMapper::class),
          getBean(ApplicationService::class),
        )

        SeedFileType.APPROVED_PREMISES_WITHDRAW_PLACEMENT_REQUEST -> Cas1WithdrawPlacementRequestSeedJob(
          getBean(PlacementRequestService::class),
          getBean(ApplicationService::class),
        )

        SeedFileType.APPROVED_PREMISES_REPLAY_DOMAIN_EVENTS -> Cas1DomainEventReplaySeedJob(
          getBean(DomainEventService::class),
        )

        SeedFileType.APPROVED_PREMISES_DUPLICATE_APPLICATION -> Cas1DuplicateApplicationSeedJob(
          getBean(ApplicationService::class),
          getBean(OffenderService::class),
        )

        SeedFileType.APPROVED_PREMISES_UPDATE_EVENT_NUMBER -> Cas1UpdateEventNumberSeedJob(
          getBean(ApplicationService::class),
          getBean(ApplicationRepository::class),
          getBean(DomainEventRepository::class),
          getBean(ObjectMapper::class),
        )

        SeedFileType.APPROVED_PREMISES_LINK_BOOKING_TO_PLACEMENT_REQUEST -> Cas1LinkedBookingToPlacementRequestSeedJob(
          getBean(PlacementRequestRepository::class),
          getBean(BookingRepository::class),
          getBean(ApplicationTimelineNoteService::class),
        )

        SeedFileType.APPROVED_PREMISES_OUT_OF_SERVICE_BEDS -> Cas1OutOfServiceBedSeedJob(
          getBean(Cas1OutOfServiceBedService::class),
          getBean(PremisesService::class),
        )

        SeedFileType.UPDATE_USERS_FROM_API -> UpdateUsersFromApiSeedJob(
          getBean(UserService::class),
        )

        SeedFileType.APPROVED_PREMISES_CRU_MANAGEMENT_AREAS -> Cas1CruManagementAreaSeedJob(
          getBean(Cas1CruManagementAreaRepository::class),
        )

        SeedFileType.APPROVED_PREMISES_BOOKING_TO_SPACE_BOOKING -> Cas1BookingToSpaceBookingSeedJob(
          getBean(ApprovedPremisesRepository::class),
          getBean(Cas1SpaceBookingRepository::class),
          getBean(BookingRepository::class),
          getBean(DomainEventRepository::class),
          getBean(DomainEventService::class),
          getBean(UserRepository::class),
          getBean(TransactionTemplate::class),
          getBean(Cas1DeliusBookingImportRepository::class),
          getBean(DepartureReasonRepository::class),
          getBean(MoveOnCategoryRepository::class),
          getBean(NonArrivalReasonRepository::class),
          getBean(EnvironmentService::class),
          getBean(PlacementRequestRepository::class),
        )

        SeedFileType.APPROVED_PREMISES_SPACE_PLANNING_DRY_RUN -> Cas1PlanSpacePlanningDryRunSeedJob(
          getBean(SpacePlanningService::class),
          getBean(Cas1PremisesService::class),
        )

        SeedFileType.APPROVED_PREMISES_IMPORT_DELIUS_BOOKING_MANAGEMENT_DATA -> Cas1ImportDeliusBookingDataSeedJob(
          getBean(NamedParameterJdbcTemplate::class),
          getBean(Cas1DeliusBookingImportRepository::class),
        )
      }

      val seedStarted = LocalDateTime.now()

      val rowsProcessed = if (job.runInTransaction) {
        transactionTemplate.execute { processJob(job, resolveCsvPath) }
      } else {
        processJob(job, resolveCsvPath)
      }

      val timeTaken = ChronoUnit.MILLIS.between(seedStarted, LocalDateTime.now())
      seedLogger.info("Seed request complete. Took $timeTaken millis and processed $rowsProcessed rows")
    } catch (exception: Exception) {
      seedLogger.error("Unable to complete Seed Job", exception)
    }
  }

  private fun <T : Any> getBean(clazz: KClass<T>) = applicationContext.getBean(clazz.java)

  private fun <T> processJob(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String): Int {
    // During processing, the CSV file is processed one row at a time to avoid OOM issues.
    // It is preferable to fail fast rather than processing half of a file before stopping,
    // so we first do a full pass but only deserializing each row
    seedLogger.info("Processing CSV file ${Path.of(job.resolveCsvPath()).absolutePathString()}")
    enforcePresenceOfRequiredHeaders(job, resolveCsvPath)
    ensureCsvCanBeDeserialized(job, resolveCsvPath)

    job.preSeed()
    val rowsProcessed = processCsv(job, resolveCsvPath)
    job.postSeed()

    return rowsProcessed
  }

  private fun <T> processCsv(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String): Int {
    var rowNumber = 1
    val errors = mutableListOf<String>()

    try {
      csvReader().open(job.resolveCsvPath()) {
        readAllWithHeaderAsSequence().forEach { row ->
          val deserializedRow = job.deserializeRow(row)
          try {
            job.processRow(deserializedRow)
          } catch (exception: RuntimeException) {
            val rootCauseException = findRootCause(exception)
            errors.add("Error on row $rowNumber: ${exception.message} ${if (rootCauseException != null) rootCauseException.message else "no exception cause"}")
            seedLogger.error("Error on row $rowNumber:", exception)
          }

          rowNumber += 1
        }
      }
    } catch (exception: Exception) {
      throw RuntimeException("Unable to process CSV at row $rowNumber", exception)
    }
    if (errors.isNotEmpty()) {
      seedLogger.error("The following row-level errors were raised: ${errors.joinToString("\n")}")
    }

    return rowNumber - 1
  }

  private fun <T> enforcePresenceOfRequiredHeaders(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String) {
    seedLogger.info("Checking that required headers are present...")

    val headerRow = try {
      csvReader().open(job.resolveCsvPath()) {
        readAllWithHeaderAsSequence().first().keys
      }
    } catch (exception: Exception) {
      throw RuntimeException("There was an issue opening the CSV file", exception)
    }

    try {
      job.verifyPresenceOfRequiredHeaders(headerRow)
    } catch (exception: Exception) {
      throw RuntimeException("The headers provided: $headerRow did not include ${exception.message}")
    }
  }

  private fun <T> ensureCsvCanBeDeserialized(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String) {
    seedLogger.info("Validating that CSV can be fully read")
    var rowNumber = 1
    val errors = mutableListOf<String>()

    try {
      csvReader().open(job.resolveCsvPath()) {
        readAllWithHeaderAsSequence().forEach { row ->
          try {
            job.deserializeRow(row)
          } catch (exception: Exception) {
            errors += "Unable to deserialize CSV at row: $rowNumber: ${exception.message} ${exception.stackTrace.joinToString("\n")}"
          }

          rowNumber += 1
        }
      }
    } catch (exception: Exception) {
      throw RuntimeException("There was an issue opening the CSV file", exception)
    }

    if (errors.any()) {
      throw RuntimeException("There were issues deserializing the CSV:\n${errors.joinToString(", \n")}")
    }
  }
}
