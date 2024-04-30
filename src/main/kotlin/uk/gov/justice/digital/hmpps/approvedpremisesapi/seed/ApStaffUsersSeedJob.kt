package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.util.UUID

class ApStaffUsersSeedJob(
  fileName: String,
  private val userService: UserService,
) : SeedJob<ApStaffUserSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf(
    "deliusUsername",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = ApStaffUserSeedCsvRow(
    deliusUsername = columns["deliusUsername"]!!.trim().uppercase(),
  )

  @SuppressWarnings("TooGenericExceptionThrown", "TooGenericExceptionCaught")
  override fun processRow(row: ApStaffUserSeedCsvRow) {
    log.info("Processing AP Staff seeding for ${row.deliusUsername}")

    val user = try {
      userService.getExistingUserOrCreate(row.deliusUsername)
    } catch (exception: Exception) {
      throw RuntimeException("Could not get user ${row.deliusUsername}", exception)
    }
  }
}

data class ApStaffUserSeedCsvRow(
  val deliusUsername: String,
)
