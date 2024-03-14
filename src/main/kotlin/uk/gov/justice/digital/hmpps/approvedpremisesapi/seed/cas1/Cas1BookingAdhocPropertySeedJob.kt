package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

class Cas1BookingAdhocPropertySeedJob(
  fileName: String,
  private val bookingRepository: BookingRepository,
) : SeedJob<Cas1BookingAdhocPropertySeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf(
    "adhoc_booking_id",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1BookingAdhocPropertySeedCsvRow(
    adhocBookingId = columns["adhoc_booking_id"]!!.trim(),
  )

  override fun processRow(row: Cas1BookingAdhocPropertySeedCsvRow) {
    val bookingId = row.adhocBookingId

    if (bookingRepository.updateBookingAsAdhoc(UUID.fromString(bookingId)) == 0) {
      error("Could not find booking with id $bookingId")
    }

    log.info("Update booking $bookingId to indicate it's adhoc")
  }
}

data class Cas1BookingAdhocPropertySeedCsvRow(
  val adhocBookingId: String,
)
