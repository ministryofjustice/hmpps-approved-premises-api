package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID
import java.util.regex.Pattern

@Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
@Component
class ShortTermAccommodationCreateOmusSeedJob(
  private val repository: OffenderManagementUnitRepository,
) : SeedJob<OmuSeedCsvRow>(
  requiredHeaders = setOf("prisonCode", "prisonName", "email"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = OmuSeedCsvRow(
    prisonCode = columns["prisonCode"]!!.trim().uppercase(),
    prisonName = columns["prisonName"]!!.trim(),
    email = columns["email"]!!.trim(),
  )

  override fun preSeed() {
    log.info("Wiping old repository")
    repository.deleteAll()
  }

  override fun processRow(row: OmuSeedCsvRow) {
    log.info("Setting up ${row.prisonCode}")

    if (validatePrisonCode(row.prisonCode, row) && validatePrisonName(row.prisonName, row) && validateEmail(row.email, row)) {
      createOffenderManagementUnit(row)
    }
  }

  private fun createOffenderManagementUnit(row: OmuSeedCsvRow) {
    repository.save(
      OffenderManagementUnitEntity(
        id = UUID.randomUUID(),
        prisonCode = row.prisonCode,
        prisonName = row.prisonName,
        email = row.email,
      ),
    )
  }

  private fun validatePrisonCode(prisonCode: String, row: OmuSeedCsvRow): Boolean {
    if (prisonCode.isNotEmpty()) {
      val regex = "^[A-Z0-9]{3,4}$"
      if (Pattern.compile(regex)
          .matcher(prisonCode)
          .matches()
      ) {
        return true
      } else {
        log.info("Skipping row ${row.prisonCode}, ${row.prisonName}, ${row.email} as prisonCode is not alphanumeric and 3 or 4 characters")
      }
    } else {
      log.info("Skipping row ${row.prisonCode}, ${row.prisonName}, ${row.email} as prisonCode is empty")
    }
    return false
  }

  private fun validateEmail(email: String, row: OmuSeedCsvRow): Boolean {
    if (email.isNotEmpty()) {
      val regex = Regex("^.*@[a-zA-Z0-9.-]*[a-zA-Z0-9]$")
      if (regex.matches(email)) {
        return true
      } else {
        log.info("Skipping row ${row.prisonCode}, ${row.prisonName}, ${row.email} as email is not correct format")
      }
    } else {
      log.info("Skipping row ${row.prisonCode}, ${row.prisonName}, ${row.email} as email is empty")
    }
    return false
  }

  private fun validatePrisonName(prisonName: String, row: OmuSeedCsvRow): Boolean {
    if (prisonName.isNotEmpty()) {
      return true
    } else {
      log.info("Skipping row ${row.prisonCode}, ${row.prisonName}, ${row.email} as prisonName is empty")
    }
    return false
  }
}

data class OmuSeedCsvRow(
  val prisonCode: String,
  val prisonName: String,
  val email: String,
)
