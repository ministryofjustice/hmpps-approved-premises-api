package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import java.util.UUID

class NomisUsersSeedJob(
  fileName: String,
  private val userService: NomisUserService,
) : SeedJob<NomisUserSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf("nomisUsername"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = NomisUserSeedCsvRow(
    nomisUsername = columns["nomisUsername"]!!.trim().uppercase(),
  )

  override fun processRow(row: NomisUserSeedCsvRow) {
    log.info("Setting up ${row.nomisUsername}")

    try {
      userService.getUserForUsername(row.nomisUsername)
    } catch (exception: Exception) {
      throw RuntimeException("Could not get user ${row.nomisUsername}", exception)
    }
  }
}

data class NomisUserSeedCsvRow(
  val nomisUsername: String,
)
