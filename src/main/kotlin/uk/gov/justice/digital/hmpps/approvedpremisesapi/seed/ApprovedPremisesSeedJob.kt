package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import java.util.UUID

class ApprovedPremisesSeedJob(
  fileName: String,
  private val premisesRepository: PremisesRepository
) : SeedJob<ApprovedPremisesSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredColumns = 12
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = ApprovedPremisesSeedCsvRow(
    id = UUID.fromString(columns["id"]!!),
    name = columns["name"]!!,
    addressLine1 = columns["addressLine1"]!!,
    postcode = columns["postcode"]!!,
    totalBeds = Integer.parseInt(columns["totalBeds"]!!),
    notes = columns["notes"]!!,
    probationRegionId = UUID.fromString(columns["probationRegionId"]!!),
    localAuthorityAreaId = UUID.fromString(columns["localAuthorityAreaId"]),
    characteristicIds = columns["characteristicIds"]!!.split(",").map { UUID.fromString(it.trim()) },
    status = PropertyStatus.valueOf(columns["status"]!!),
    apCode = columns["apCode"]!!,
    qCode = columns["qCode"]!!
  )

  override fun processRow(row: ApprovedPremisesSeedCsvRow) = log.info("TODO: Process AP Premises row: $row")
}

data class ApprovedPremisesSeedCsvRow(
  val id: UUID,
  val name: String,
  val addressLine1: String,
  val postcode: String,
  val totalBeds: Int,
  val notes: String,
  val probationRegionId: UUID,
  val localAuthorityAreaId: UUID,
  val characteristicIds: List<UUID>,
  val status: PropertyStatus,
  val apCode: String,
  val qCode: String
)
