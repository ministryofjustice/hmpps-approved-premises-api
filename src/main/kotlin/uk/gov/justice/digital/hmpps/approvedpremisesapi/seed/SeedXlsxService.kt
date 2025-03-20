package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1SeedPremisesFromSiteSurveyXlsxJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1SeedRoomsFromSiteSurveyXlsxJob
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.reflect.KClass

@Service
class SeedXlsxService(
  private val seedConfig: SeedConfig,
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val seedLogger: SeedLogger,
) {

  val filePrefix = seedConfig.filePrefix

  fun seedDirectoryRecursive(fileType: SeedFromExcelFileType, directory: String) {
    seedLogger.info("Starting seed request for directory: $fileType - $directory")

    if (!seedPathIsValid(directory)) {
      seedLogger.error("Invalid directory $directory. Should be the name of a file within $filePrefix. Sub directories are not allowed")
      return
    }

    val path = Paths.get(filePrefix, directory)
    if (!path.isDirectory()) {
      seedLogger.error("Cannot find directory '$directory' within $filePrefix")
      return
    }

    Files.walk(path)
      .filter(Files::isRegularFile)
      .filter { it.name.endsWith(".xlsx", ignoreCase = true) }
      .forEach {
        seedLogger.info("Seeding file ${it.name} from directory ${it.parent.name}")
        seed(fileType, it.toFile())
      }
  }

  fun seedFile(excelSeedFileType: SeedFromExcelFileType, filename: String) {
    seedLogger.info("Starting seed request: $excelSeedFileType - $filename")

    if (!seedPathIsValid(filename)) {
      seedLogger.error("Invalid filename $filename. Should be the name of a file within $filePrefix. Sub directories are not allowed")
      return
    }

    seed(excelSeedFileType, File("${seedConfig.filePrefix}/$filename"))
  }

  @SuppressWarnings("TooGenericExceptionThrown", "TooGenericExceptionCaught")
  private fun seed(excelSeedFileType: SeedFromExcelFileType, file: File) {
    try {
      val job = when (excelSeedFileType) {
        SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS -> getBean(Cas1SeedRoomsFromSiteSurveyXlsxJob::class)
        SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_PREMISES -> getBean(Cas1SeedPremisesFromSiteSurveyXlsxJob::class)
      }

      val seedStarted = LocalDateTime.now()

      transactionTemplate.executeWithoutResult {
        seedLogger.info("Processing XLSX file ${file.absolutePath}")
        job.processXlsx(file)
      }

      val timeTaken = ChronoUnit.MILLIS.between(seedStarted, LocalDateTime.now())
      seedLogger.info("Excel seed request complete. Took $timeTaken millis")
    } catch (exception: Throwable) {
      seedLogger.error("Unable to complete Excel seed job for '${file.name}' with message '${exception.message}'", exception)
    }
  }

  private fun seedPathIsValid(filename: String) = !filename.contains("/") && !filename.contains("\\")

  private fun <T : Any> getBean(clazz: KClass<T>) = applicationContext.getBean(clazz.java)
}
