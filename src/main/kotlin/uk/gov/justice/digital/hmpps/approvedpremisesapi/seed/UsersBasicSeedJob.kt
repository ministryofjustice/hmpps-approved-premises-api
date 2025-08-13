package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService.GetUserResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
/**
 * Seeds users, without touching any existing user's roles and qualifications.
 *
 * If you want to set roles and qualifications as part of the seeding then look at [UsersSeedJob].
 */
@Component
class UsersBasicSeedJob(
  private val userService: UserService,
  private val staffMemberService: StaffMemberService,
  private val seedLogger: SeedLogger,
) : SeedJob<ApStaffUserSeedCsvRow>() {

  override fun deserializeRow(columns: Map<String, String>): ApStaffUserSeedCsvRow {
    val seedColumns = SeedColumns(columns)
    return ApStaffUserSeedCsvRow(
      deliusUsername = seedColumns.getStringOrNull("deliusUsername")?.trim()?.uppercase(),
      staffCode = seedColumns.getStringOrNull("staffCode")?.trim()?.uppercase(),
    )
  }

  @SuppressWarnings("TooGenericExceptionThrown", "TooGenericExceptionCaught")
  override fun processRow(row: ApStaffUserSeedCsvRow) {
    if (row.deliusUsername == null && row.staffCode == null) {
      error("Must provide either a delius username or a staff code")
    }

    val username = row.deliusUsername ?: resolveUsernameForStaffCode(row.staffCode!!)

    seedLogger.info("Ensuring user exists with username '$username'")

    val result = try {
      when (val result = userService.getExistingUserOrCreate(username)) {
        GetUserResponse.StaffRecordNotFound -> throw SeedException("Could not find staff record for user $username")
        is GetUserResponse.StaffProbationRegionNotSupported -> throw SeedException("Probation region ${result.unsupportedRegionId} not supported for user $username")
        is GetUserResponse.Success -> result
      }
    } catch (exception: Exception) {
      throw SeedException("Could not get user $username", exception)
    }
    seedLogger.info(seedingReport(result))
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun resolveUsernameForStaffCode(staffCode: String): String {
    try {
      val username = extractEntityFromCasResult(staffMemberService.getStaffMemberByCode(staffCode)).username?.uppercase()
      if (username == null) {
        error("Could not resolve username for staff code $staffCode as no username value found")
      }
      seedLogger.info("Have resolved username $username for staff code $staffCode")
      return username
    } catch (e: Exception) {
      throw SeedException("Could not resolve username for staff code $staffCode", e)
    }
  }

  private fun seedingReport(response: GetUserResponse.Success): String {
    val user = response.user
    val username = user.deliusUsername
    if (response.createdOnGet) {
      return "-> User record for '$username' created"
    } else {
      val timestamp = user.updatedAt ?: user.createdAt
      val ageInMinutes = ChronoUnit.MINUTES.between(timestamp, OffsetDateTime.now())
      return "-> User record for '$username' already exists. Last updated $ageInMinutes mins ago"
    }
  }
}

data class ApStaffUserSeedCsvRow(
  val deliusUsername: String?,
  val staffCode: String?,
)
