package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class ApStaffUsersSeedJob(
  fileName: String,
  private val userService: UserService,
  private val seedLogger: SeedLogger,
) : SeedJob<ApStaffUserSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf(
    "deliusUsername",
  ),
) {

  override fun deserializeRow(columns: Map<String, String>) = ApStaffUserSeedCsvRow(
    deliusUsername = columns["deliusUsername"]!!.trim().uppercase(),
  )

  @SuppressWarnings("TooGenericExceptionThrown", "TooGenericExceptionCaught")
  override fun processRow(row: ApStaffUserSeedCsvRow) {
    seedLogger.info("Processing AP Staff seeding for ${row.deliusUsername}")

    val user = try {
      userService.getExistingUserOrCreate(row.deliusUsername)
    } catch (exception: Exception) {
      throw RuntimeException("Could not get user ${row.deliusUsername}", exception)
    }
    seedLogger.info(seedingReport(user))
  }

  private fun seedingReport(user: UserEntity): String {
    val ageInMinutes = ChronoUnit.MINUTES.between(user.createdAt, OffsetDateTime.now())
    val actionTaken = if (ageInMinutes > 1) "Found pre-existing" else "Seeded"

    return "-> $actionTaken: ${user.deliusUsername} (created $ageInMinutes mins ago)"
  }
}

data class ApStaffUserSeedCsvRow(
  val deliusUsername: String,
)
