package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob

@Component
class Cas1RemapBedCodesSeedService(
  private val bedRepository: BedRepository,
) : SeedJob<Cas1RemapBedCodesSeedCsvRow>(
  requiredHeaders = setOf(
    "premises_name",
    "old_bed_code",
    "new_bed_code",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1RemapBedCodesSeedCsvRow {
    val seedColumns = SeedColumns(columns)

    return Cas1RemapBedCodesSeedCsvRow(
      premisesName = seedColumns.getStringOrNull("premises_name")!!,
      oldBedCode = seedColumns.getStringOrNull("old_bed_code")!!,
      newBedCode = seedColumns.getStringOrNull("new_bed_code")!!,
    )
  }

  override fun processRow(row: Cas1RemapBedCodesSeedCsvRow) {
    val premisesName = row.premisesName
    val oldCode = row.oldBedCode
    val newCode = row.newBedCode

    val bed = bedRepository.findByCode(oldCode) ?: error("No bed found for code '$oldCode'")

    val premises = bed.room.premises
    if (premises.name != premisesName) {
      error("Expected premises with name '$premisesName' for bed '$oldCode' but found '${premises.name}'")
    }

    log.info("Updating bed code '$oldCode' in premise '$premisesName' to '$newCode'")
    bedRepository.updateCode(bed.id, newCode)
  }
}

data class Cas1RemapBedCodesSeedCsvRow(
  val premisesName: String,
  val oldBedCode: String,
  val newBedCode: String,
)
