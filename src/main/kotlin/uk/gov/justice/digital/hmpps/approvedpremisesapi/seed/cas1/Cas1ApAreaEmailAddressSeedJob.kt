package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

class Cas1ApAreaEmailAddressSeedJob(
  fileName: String,
  private val apAreaRepository: ApAreaRepository,
) : SeedJob<Cas1ApAreaEmailAddressSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf(
    "ap_area_identifier",
    "email_address",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1ApAreaEmailAddressSeedCsvRow(
    apAreaIdentifier = columns["ap_area_identifier"]!!.trim(),
    emailAddress = columns["email_address"]!!.trim(),
  )

  override fun processRow(row: Cas1ApAreaEmailAddressSeedCsvRow) {
    val apArea = apAreaRepository.findByIdentifier(row.apAreaIdentifier)
      ?: error("AP Area with identifier '${row.apAreaIdentifier}' does not exist")

    val emailAddress = row.emailAddress
    if (emailAddress.isBlank()) {
      error("Email address for '${row.apAreaIdentifier}' is blank")
    }

    apAreaRepository.updateEmailAddress(apArea.id, emailAddress)

    log.info("Updated email address for AP Area ${apArea.id} to $emailAddress")
  }
}

data class Cas1ApAreaEmailAddressSeedCsvRow(
  val apAreaIdentifier: String,
  val emailAddress: String,
)
