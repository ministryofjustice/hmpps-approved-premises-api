package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import java.util.UUID

@Component
class Cas1NonArrivalPlacementDataFixSeedJob(
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
) : SeedJob<PlacementSeedRow>(
  requiredHeaders = setOf(
    "crn",
    "space_booking_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): PlacementSeedRow {
    val seedColumns = SeedColumns(columns)
    return PlacementSeedRow(
      crn = seedColumns.getStringOrNull("crn")!!,
      spaceBookingId = seedColumns.getUuidOrNull("space_booking_id")!!,
    )
  }

  override fun processRow(row: PlacementSeedRow) {
    val crn = row.crn
    val placementId = row.spaceBookingId

    log.info("Fixing placement data for CRN $crn and placementId $placementId")
    val placement = spaceBookingRepository.findByIdOrNull(placementId)
      ?: throw SeedException("Placement with ID $placementId not found for id $placementId")

    if (placement.crn != crn) {
      throw SeedException("Placement with ID $placementId has incorrect CRN ${placement.crn}")
    }

    if (placement.hasNonArrival()) {
      log.info("Placement with ID $placementId has non arrival data")

      placement.nonArrivalConfirmedAt = null
      placement.nonArrivalNotes = null
      placement.nonArrivalReason = null

      spaceBookingRepository.save(placement)
    }
  }
}

data class PlacementSeedRow(val crn: String, val spaceBookingId: UUID)
