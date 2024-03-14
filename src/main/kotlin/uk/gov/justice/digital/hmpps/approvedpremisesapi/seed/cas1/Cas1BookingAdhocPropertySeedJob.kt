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
    "booking_id",
    "is_adhoc",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1BookingAdhocPropertySeedCsvRow(
    bookingId = columns["booking_id"]!!.trim(),
    isAdhoc = columns["is_adhoc"]!!.trim().equals("true", ignoreCase = true),
  )

  override fun processRow(row: Cas1BookingAdhocPropertySeedCsvRow) {
    val bookingId = row.bookingId
    val isAdhoc = row.isAdhoc

    if (bookingRepository.updateBookingAdhocStatus(UUID.fromString(bookingId), isAdhoc) == 0) {
      error("Could not find booking with id $bookingId")
    }

    log.info("Update booking $bookingId adhoc status to $isAdhoc")
  }
}

data class Cas1BookingAdhocPropertySeedCsvRow(
  val bookingId: String,
  val isAdhoc: Boolean,
)
