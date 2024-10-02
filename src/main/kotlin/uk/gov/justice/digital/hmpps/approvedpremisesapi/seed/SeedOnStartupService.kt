package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import jakarta.annotation.PostConstruct
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1AutoScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.Cas2AutoScript
import java.io.File
import java.io.IOException

@Service
class SeedOnStartupService(
  private val seedConfig: SeedConfig,
  private val cas1AutoScript: Cas1AutoScript,
  private val cas2AutoScript: Cas2AutoScript,
  private val seedService: SeedService,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("NestedBlockDepth")
  @PostConstruct
  fun autoSeed() {
    if (!seedConfig.auto.enabled) {
      return
    }

    log.info("Auto-seeding from locations: ${seedConfig.auto.filePrefixes}")
    for (filePrefix in seedConfig.auto.filePrefixes) {
      val csvFiles = try {
        PathMatchingResourcePatternResolver().getResources("$filePrefix/*.csv")
      } catch (e: IOException) {
        log.warn(e.message!!)
        continue
      }

      csvFiles.sortBy { it.filename }

      for (csv in csvFiles) {
        val csvName = csv.filename!!
          .replace("\\.csv$".toRegex(), "")
          .replace("^[0-9]+__".toRegex(), "")
        val seedFileType = SeedFileType.values().firstOrNull { it.value == csvName }
        if (seedFileType == null) {
          log.warn("Seed file ${csv.file.path} does not have a known job type; skipping.")
        } else {
          log.info("Found seed job of type $seedFileType in $filePrefix")
          val filePath = if (csv is ClassPathResource) {
            csv.inputStream

            val targetFile = File("${seedConfig.filePrefix}/${csv.filename}")
            log.info("Copying class path resource ${csv.filename} to ${targetFile.absolutePath}")
            FileUtils.copyInputStreamToFile(csv.inputStream, targetFile)

            targetFile.absolutePath
          } else {
            csv.file.path
          }

          seedService.seedData(seedFileType, seedFileType.value) { filePath }
        }
      }
    }

    if (seedConfig.autoScript.cas1Enabled) {
      autoScriptCas1()
    }

    if (seedConfig.autoScript.cas2Enabled) {
      autoScriptCas2()
    }
  }

  fun autoScriptCas1() {
    log.info("**Auto-scripting CAS1**")
    cas1AutoScript.script()
  }

  fun autoScriptCas2() {
    log.info("**Auto-scripting CAS2**")
    cas2AutoScript.script()
  }
}
