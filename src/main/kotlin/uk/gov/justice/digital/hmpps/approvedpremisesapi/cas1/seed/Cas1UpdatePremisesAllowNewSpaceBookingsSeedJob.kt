package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import java.util.UUID

@Service
class Cas1UpdatePremisesAllowNewSpaceBookingsSeedJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
) : SeedJob<Cas1UpdatePremisesAllowNewSpaceBookingsSeedJob.CsvRow>(
  setOf(
    "premises_id",
    "allow_new_space_bookings",
  ),
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): CsvRow {
    val seedColumns = SeedColumns(columns)

    return CsvRow(
      premisesId = seedColumns.getUuidOrNull("premises_id")!!,
      allowNewSpaceBookings = seedColumns.getYesNoBooleanOrNull("allow_new_space_bookings")!!,
    )
  }

  override fun processRow(row: CsvRow) {
    val premisesId = row.premisesId
    val allowNewSpaceBookings = row.allowNewSpaceBookings

    log.info("Updating allow new space bookings for premises id $premisesId to $allowNewSpaceBookings")

    val approvedPremises = approvedPremisesRepository.findByIdOrNull(premisesId)
      ?: error("Could not find approved premises with id $premisesId")

    approvedPremises.allowNewSpaceBookings = allowNewSpaceBookings
    approvedPremisesRepository.save(approvedPremises)
  }

  data class CsvRow(
    val premisesId: UUID,
    val allowNewSpaceBookings: Boolean,
  )
}
