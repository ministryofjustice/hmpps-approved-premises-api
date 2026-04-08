package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

@Component
class Cas1UpdatePremisesEmailSeedJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
) : SeedJob<PremisesEmailSeedRow>(
  requiredHeaders = setOf(
    "premises_id",
    "email_address",
  ),
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): PremisesEmailSeedRow {
    val seedColumns = SeedColumns(columns)
    return PremisesEmailSeedRow(
      premisesId = seedColumns.getUuidOrNull("premises_id")!!,
      emailAddress = seedColumns.getStringOrNull("email_address")!!,
    )
  }

  override fun processRow(row: PremisesEmailSeedRow) {
    val premisesId = row.premisesId
    val emailAddress = row.emailAddress

    log.info("Updating email address for premises id $premisesId")
    val premises = approvedPremisesRepository.findByIdOrNull(premisesId)
      ?: throw SeedException("Premises not found for premises ID $premisesId")

    premises.emailAddress = emailAddress
    approvedPremisesRepository.save(premises)

    log.info("Successfully updated premises email address for premises id $premisesId")
  }
}

data class PremisesEmailSeedRow(val premisesId: UUID, val emailAddress: String)
