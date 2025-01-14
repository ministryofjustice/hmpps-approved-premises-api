package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import jakarta.annotation.PostConstruct
import org.apache.commons.io.FileUtils
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1StartupScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.Cas2StartupScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import java.io.File
import java.io.IOException

@Service
class SeedOnStartupService(
  private val seedConfig: SeedConfig,
  private val cas1StartupScript: Cas1StartupScript,
  private val cas2StartupScript: Cas2StartupScript,
  private val seedService: SeedService,
  private val seedLogger: SeedLogger,
  private val environmentService: EnvironmentService,
  private val sentryService: SentryService,
) {
  @SuppressWarnings("NestedBlockDepth")
  @PostConstruct
  fun seedOnStartup() {
    val startupConfig = seedConfig.onStartup

    if (!startupConfig.enabled) {
      return
    }

    if (environmentService.isNotATestEnvironment()) {
      sentryService.captureErrorMessage("Seed on startup should not be enabled outside of local and dev environments")
      return
    }

    seedLogger.info("Seeding on startup from locations: ${startupConfig.filePrefixes}")
    for (filePrefix in startupConfig.filePrefixes) {
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

          seedService.seedData(seedFileType, seedFileType.value) { filePath }
        }
      }
    }

    if (startupConfig.script.cas1Enabled) {
      cas1StartupScript.script()
    }

    if (startupConfig.script.cas2Enabled) {
      cas2StartupScript.script()
    }
  }
}
