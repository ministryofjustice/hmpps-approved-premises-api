package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApprovedPremisesRoomsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApprovedPremisesSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CharacteristicsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.TemporaryAccommodationBedspaceSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.TemporaryAccommodationPremisesSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.UsersSeedJob

@Service
class SeedService(
  private val seedConfig: SeedConfig,
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val seedLogger: SeedLogger,
) {
  @Async
  fun seedDataAsync(seedFileType: SeedFileType, filename: String) = seedData(seedFileType, filename)

  fun seedData(seedFileType: SeedFileType, filename: String) {
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
      }

      transactionTemplate.executeWithoutResult { processJob(job) }
    } catch (exception: Exception) {
      seedLogger.error("Unable to complete Seed Job", exception)
    }
  }

  private fun <T> processJob(job: SeedJob<T>) {
    // During processing, the CSV file is processed one row at a time to avoid OOM issues.
    // It is preferable to fail fast rather than processing half of a file before stopping,
    // so we first do a full pass but only deserializing each row
    enforcePresenceOfRequiredHeaders(job)
    ensureCsvCanBeDeserialized(job)
    processCsv(job)
  }

  private fun <T> processCsv(job: SeedJob<T>) {
    var rowNumber = 1
    val errors = mutableListOf<String>()

    try {
      csvReader().open("${seedConfig.filePrefix}/${job.fileName}.csv") {
        readAllWithHeaderAsSequence().forEach { row ->
          val deserializedRow = job.deserializeRow(row)
          try {
            job.processRow(deserializedRow)
          } catch (exception: RuntimeException) {
            errors.add("Error on row $rowNumber: ${exception.message}")
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

  private fun <T> enforcePresenceOfRequiredHeaders(job: SeedJob<T>) {
    seedLogger.info("Checking that required headers are present...")

    val headerRow = try {
      csvReader().open("${seedConfig.filePrefix}/${job.fileName}.csv") {
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

  private fun <T> ensureCsvCanBeDeserialized(job: SeedJob<T>) {
    seedLogger.info("Validating that CSV can be fully read")
    var rowNumber = 1
    val errors = mutableListOf<String>()

    try {
      csvReader().open("${seedConfig.filePrefix}/${job.fileName}.csv") {
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
