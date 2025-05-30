package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

@Service
class Cas1UpdatePremisesStatusSeedJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
) : SeedJob<Cas1UpdatePremisesStatusSeedJobCsvRow>(
  requiredHeaders = setOf(
    "premises_id",
    "status",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1UpdatePremisesStatusSeedJobCsvRow {
    val seedColumns = SeedColumns(columns)
    return Cas1UpdatePremisesStatusSeedJobCsvRow(
      premisesId = seedColumns.getUuidOrNull("premises_id")!!,
      status = PropertyStatus.valueOf(seedColumns.getStringOrNull("status")!!),
    )
  }

  override fun processRow(row: Cas1UpdatePremisesStatusSeedJobCsvRow) {
    val premisesId = row.premisesId
    val updatedStatus = row.status

    log.info("Updating Approved Premises '$premisesId' status to '$updatedStatus'")

    val premises = approvedPremisesRepository.findByIdOrNull(premisesId)
      ?: error("Approved Premises with identifier '$premisesId' does not exist")

    val previousValue = premises.status
    premises.status = updatedStatus

    approvedPremisesRepository.save(premises)

    log.info("Have updated status for approved premises ${premises.name} from '$previousValue' to '$updatedStatus'")
  }
}

data class Cas1UpdatePremisesStatusSeedJobCsvRow(
  val premisesId: UUID,
  val status: PropertyStatus,
)
