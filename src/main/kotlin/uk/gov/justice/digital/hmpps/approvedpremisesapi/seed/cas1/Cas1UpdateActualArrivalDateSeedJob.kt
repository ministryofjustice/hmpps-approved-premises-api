package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1UpdateActualArrivalDateSeedJob(
  val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  val applicationTimelineNoteService: ApplicationTimelineNoteService,
) : SeedJob<Cas1UpdateActualArrivalDateSeedJobCsvRow>(
  requiredHeaders = setOf(
    "space_booking_id",
    "current_arrival_date",
    "updated_arrival_date",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1UpdateActualArrivalDateSeedJobCsvRow {
    val seedColumns = SeedColumns(columns)

    return Cas1UpdateActualArrivalDateSeedJobCsvRow(
      spaceBookingId = seedColumns.getUuidOrNull("space_booking_id")!!,
      currentArrivalDate = seedColumns.getLocalDateOrNull("current_arrival_date")!!,
      updatedArrivalDate = seedColumns.getLocalDateOrNull("updated_arrival_date")!!,
    )
  }

  override fun processRow(row: Cas1UpdateActualArrivalDateSeedJobCsvRow) {
    val id = row.spaceBookingId
    val currentDate = row.currentArrivalDate
    val updatedDate = row.updatedArrivalDate

    val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(id) ?: error("Could not find space booking with id $id")

    if (spaceBooking.actualArrivalDate != currentDate) {
      error("Expected current actual arrival date to be $currentDate, but was actually ${spaceBooking.actualArrivalDate}")
    }

    log.info("Updating actual and canonical arrival date on ${spaceBooking.id} to $updatedDate")

    spaceBooking.actualArrivalDate = updatedDate
    spaceBooking.canonicalArrivalDate = updatedDate

    cas1SpaceBookingRepository.save(spaceBooking)

    spaceBooking.application?.let {
      applicationTimelineNoteService.saveApplicationTimelineNote(
        applicationId = it.id,
        note = "Actual arrival date for booking at '${spaceBooking.premises.name}' has been updated from $currentDate to $updatedDate by application support",
        user = null,
      )
    }
  }
}

data class Cas1UpdateActualArrivalDateSeedJobCsvRow(
  val spaceBookingId: UUID,
  val currentArrivalDate: LocalDate,
  val updatedArrivalDate: LocalDate,
)
