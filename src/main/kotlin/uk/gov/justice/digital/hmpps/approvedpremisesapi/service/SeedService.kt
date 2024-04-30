package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.apache.commons.io.FileUtils
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApStaffUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApprovedPremisesBookingCancelSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApprovedPremisesBookingSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApprovedPremisesOfflineApplicationsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApprovedPremisesRoomsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApprovedPremisesSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CharacteristicsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.TemporaryAccommodationBedspaceSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.TemporaryAccommodationPremisesSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.UsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1ApAreaEmailAddressSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingAdhocPropertySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1DomainEventReplaySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1DuplicateApplicationSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1FurtherInfoBugFixSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1RemoveAssessmentDetailsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1WithdrawPlacementRequestSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.Cas2ApplicationsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.Cas2AutoScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.ExternalUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.NomisUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.findRootCause
import java.io.File
import java.io.IOException
import java.nio.file.Path
import javax.annotation.PostConstruct
import kotlin.io.path.absolutePathString

@Service
class SeedService(
  private val seedConfig: SeedConfig,
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val seedLogger: SeedLogger,
  private val cas2AutoScript: Cas2AutoScript,
) {

  @PostConstruct
  fun autoSeed() {
    if (!seedConfig.auto.enabled) {
      return
    }

    seedLogger.info("Auto-seeding from locations: ${seedConfig.auto.filePrefixes}")
    for (filePrefix in seedConfig.auto.filePrefixes) {
      val csvFiles = try {
        PathMatchingResourcePatternResolver().getResources("$filePrefix/*.csv")
      } catch (e: IOException) {
        seedLogger.warn(e.message!!)
        continue
      }

      csvFiles.sortBy { it.filename }

      for (csv in csvFiles) {
        val csvName = csv.filename!!
          .replace("\\.csv$".toRegex(), "")
          .replace("^[0-9]+__".toRegex(), "")
        val seedFileType = SeedFileType.values().firstOrNull { it.value == csvName }
        if (seedFileType == null) {
          seedLogger.warn("Seed file ${csv.file.path} does not have a known job type; skipping.")
        } else {
          seedLogger.info("Found seed job of type $seedFileType in $filePrefix")
          val filePath = if (csv is ClassPathResource) {
            csv.inputStream

            val targetFile = File("${seedConfig.filePrefix}/${csv.filename}")
            seedLogger.info("Copying class path resource ${csv.filename} to ${targetFile.absolutePath}")
            FileUtils.copyInputStreamToFile(csv.inputStream, targetFile)

            targetFile.absolutePath
          } else {
            csv.file.path
          }

          seedData(seedFileType, seedFileType.value) { filePath }
        }
      }
    }

    if (seedConfig.autoScript.enabled) {
      this.autoScript()
    }
  }

  fun autoScript() {
    seedLogger.info("**Auto-scripting**")
    cas2AutoScript.script()
  }

  @Async
  fun seedDataAsync(seedFileType: SeedFileType, filename: String) = seedData(seedFileType, filename)

  fun seedData(seedFileType: SeedFileType, filename: String) = seedData(seedFileType, filename) { "${seedConfig.filePrefix}/${this.fileName}.csv" }

  private fun seedData(seedFileType: SeedFileType, filename: String, resolveCsvPath: SeedJob<*>.() -> String) {
    seedLogger.info("Starting seed request: $seedFileType - $filename")

    try {
      val job: SeedJob<*> = when (seedFileType) {
        SeedFileType.approvedPremises -> ApprovedPremisesSeedJob(
          filename,
          applicationContext.getBean(PremisesRepository::class.java),
          applicationContext.getBean(ProbationRegionRepository::class.java),
          applicationContext.getBean(LocalAuthorityAreaRepository::class.java),
          applicationContext.getBean(CharacteristicRepository::class.java),
        )
        SeedFileType.approvedPremisesRooms -> ApprovedPremisesRoomsSeedJob(
          filename,
          applicationContext.getBean(PremisesRepository::class.java),
          applicationContext.getBean(RoomRepository::class.java),
          applicationContext.getBean(BedRepository::class.java),
          applicationContext.getBean(CharacteristicRepository::class.java),
        )
        SeedFileType.user -> UsersSeedJob(
          filename,
          ServiceName.values().toList(),
          applicationContext.getBean(UserService::class.java),
        )
        SeedFileType.approvedPremisesApStaffUsers -> ApStaffUsersSeedJob(
          filename,
          applicationContext.getBean(UserService::class.java),
        )
        SeedFileType.nomisUsers -> NomisUsersSeedJob(
          filename,
          applicationContext.getBean(NomisUserRepository::class.java),
        )
        SeedFileType.externalUsers -> ExternalUsersSeedJob(
          filename,
          applicationContext.getBean(ExternalUserRepository::class.java),
        )
        SeedFileType.cas2Applications -> Cas2ApplicationsSeedJob(
          filename,
          applicationContext.getBean(Cas2ApplicationRepository::class.java),
          applicationContext.getBean(NomisUserRepository::class.java),
          applicationContext.getBean(ExternalUserRepository::class.java),
          applicationContext.getBean(Cas2StatusUpdateRepository::class.java),
          applicationContext.getBean(Cas2AssessmentRepository::class.java),
          applicationContext.getBean(JsonSchemaService::class.java),
          applicationContext.getBean(Cas2PersistedApplicationStatusFinder::class.java),
        )
        SeedFileType.approvedPremisesUsers -> UsersSeedJob(
          filename,
          listOf(ServiceName.approvedPremises),
          applicationContext.getBean(UserService::class.java),
        )
        SeedFileType.temporaryAccommodationUsers -> UsersSeedJob(
          filename,
          listOf(ServiceName.temporaryAccommodation),
          applicationContext.getBean(UserService::class.java),
        )
        SeedFileType.characteristics -> CharacteristicsSeedJob(
          filename,
          applicationContext.getBean(CharacteristicRepository::class.java),
        )
        SeedFileType.temporaryAccommodationPremises -> TemporaryAccommodationPremisesSeedJob(
          filename,
          applicationContext.getBean(PremisesRepository::class.java),
          applicationContext.getBean(ProbationRegionRepository::class.java),
          applicationContext.getBean(LocalAuthorityAreaRepository::class.java),
          applicationContext.getBean(ProbationDeliveryUnitRepository::class.java),
          applicationContext.getBean(CharacteristicService::class.java),
        )
        SeedFileType.temporaryAccommodationBedspace -> TemporaryAccommodationBedspaceSeedJob(
          filename,
          applicationContext.getBean(PremisesRepository::class.java),
          applicationContext.getBean(CharacteristicService::class.java),
          applicationContext.getBean(RoomService::class.java),
        )

        SeedFileType.approvedPremisesOfflineApplications -> ApprovedPremisesOfflineApplicationsSeedJob(
          filename,
          applicationContext.getBean(OfflineApplicationRepository::class.java),
        )

        SeedFileType.approvedPremisesBookings -> ApprovedPremisesBookingSeedJob(
          filename,
          applicationContext.getBean(BookingRepository::class.java),
          applicationContext.getBean(BookingService::class.java),
          applicationContext.getBean(CommunityApiClient::class.java),
          applicationContext.getBean(BedRepository::class.java),
          applicationContext.getBean(DepartureReasonRepository::class.java),
          applicationContext.getBean(MoveOnCategoryRepository::class.java),
          applicationContext.getBean(DestinationProviderRepository::class.java),
          applicationContext.getBean(NonArrivalReasonRepository::class.java),
          applicationContext.getBean(CancellationReasonRepository::class.java),
        )

        SeedFileType.approvedPremisesCancelBookings -> ApprovedPremisesBookingCancelSeedJob(
          filename,
          applicationContext.getBean(BookingService::class.java),
          applicationContext.getBean(BookingRepository::class.java),
        )

        SeedFileType.approvedPremisesApAreaEmailAddresses -> Cas1ApAreaEmailAddressSeedJob(
          filename,
          applicationContext.getBean(ApAreaRepository::class.java),
        )

        SeedFileType.approvedPremisesBookingAdhocProperty -> Cas1BookingAdhocPropertySeedJob(
          filename,
          applicationContext.getBean(BookingRepository::class.java),
        )

        SeedFileType.approvedPremisesAssessmentMoreInfoBugFix -> Cas1FurtherInfoBugFixSeedJob(
          filename,
          applicationContext.getBean(AssessmentRepository::class.java),
        )

        SeedFileType.approvedPremisesRedactAssessmentDetails -> Cas1RemoveAssessmentDetailsSeedJob(
          filename,
          applicationContext.getBean(AssessmentRepository::class.java),
          applicationContext.getBean(ObjectMapper::class.java),
          applicationContext.getBean(ApplicationService::class.java),
        )

        SeedFileType.approvedPremisesWithdrawPlacementRequest -> Cas1WithdrawPlacementRequestSeedJob(
          filename,
          applicationContext.getBean(PlacementRequestService::class.java),
          applicationContext.getBean(ApplicationService::class.java),
        )

        SeedFileType.approvedPremisesReplayDomainEvents -> Cas1DomainEventReplaySeedJob(
          filename,
          applicationContext.getBean(DomainEventService::class.java),
        )

        SeedFileType.approvedPremisesDuplicateApplication -> Cas1DuplicateApplicationSeedJob(
          filename,
          applicationContext.getBean(ApplicationService::class.java),
          applicationContext.getBean(OffenderService::class.java),
        )
      }

      transactionTemplate.executeWithoutResult { processJob(job, resolveCsvPath) }

      seedLogger.info("Seed request complete")
    } catch (exception: Exception) {
      seedLogger.error("Unable to complete Seed Job", exception)
    }
  }

  private fun <T> processJob(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String) {
    // During processing, the CSV file is processed one row at a time to avoid OOM issues.
    // It is preferable to fail fast rather than processing half of a file before stopping,
    // so we first do a full pass but only deserializing each row
    seedLogger.info("Processing CSV file ${Path.of(job.resolveCsvPath()).absolutePathString()}")
    enforcePresenceOfRequiredHeaders(job, resolveCsvPath)
    ensureCsvCanBeDeserialized(job, resolveCsvPath)
    processCsv(job, resolveCsvPath)
  }

  private fun <T> processCsv(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String) {
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
