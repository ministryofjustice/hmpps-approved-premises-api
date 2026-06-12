package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import java.util.UUID

@Service
class Cas1UpdatePremisesSupportSpaceBookingSeedJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
) : SeedJob<Cas1UpdatePremisesSupportSpaceBookingSeedJobCsvRow>(
  setOf(
    "premises_id",
    "support_space_booking",
  ),
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1UpdatePremisesSupportSpaceBookingSeedJobCsvRow {
    val seedColumns = SeedColumns(columns)

    return Cas1UpdatePremisesSupportSpaceBookingSeedJobCsvRow(
      premisesId = seedColumns.getUuidOrNull("premises_id")!!,
      supportSpaceBooking = seedColumns.getYesNoBooleanOrNull("support_space_booking") ?: false,
    )
  }

  override fun processRow(row: Cas1UpdatePremisesSupportSpaceBookingSeedJobCsvRow) {
    val premisesId = row.premisesId
    val supportSpaceBooking = row.supportSpaceBooking

    log.info("Updating support space booking for premises id $premisesId to $supportSpaceBooking")

    val approvedPremises = approvedPremisesRepository.findByIdOrNull(premisesId)
      ?: error("Could not find approved premises with id $premisesId")

    approvedPremises.supportsSpaceBookings = supportSpaceBooking
    approvedPremisesRepository.save(approvedPremises)
  }
}

data class Cas1UpdatePremisesSupportSpaceBookingSeedJobCsvRow(
  val premisesId: UUID,
  val supportSpaceBooking: Boolean,
)
