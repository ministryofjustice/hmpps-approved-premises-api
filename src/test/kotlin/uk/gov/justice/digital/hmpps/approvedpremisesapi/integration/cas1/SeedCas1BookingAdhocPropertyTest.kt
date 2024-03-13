package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1BookingAdhocPropertyTest : SeedTestBase() {

  @Test
  fun `Attempting to seed booking with an invalid booking id logs an error`() {
    val invalidId = UUID.randomUUID()

    withCsv(
      "invalid-booking-id",
      bookingIdListToCsvRows(listOf(invalidId))
    )

    seedService.seedData(SeedFileType.approvedPremisesBookingAdhocProperty, "invalid-booking-id")

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable != null &&
          it.throwable.message == "Could not find booking with id $invalidId"
      }
  }

  @Test
  fun `Bookings are updated to be adhoc`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }
    val booking1 = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
    }
    val booking2 = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
    }
    val booking3 = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
    }

    withCsv(
      "valid-booking-ids",
      bookingIdListToCsvRows(listOf(
        booking1.id,
        booking3.id,
      ))
    )

    seedService.seedData(SeedFileType.approvedPremisesBookingAdhocProperty, "valid-booking-ids")

    assertThat(bookingRepository.findByIdOrNull(booking1.id)!!.adhoc).isTrue()
    assertThat(bookingRepository.findByIdOrNull(booking2.id)!!.adhoc).isNull()
    assertThat(bookingRepository.findByIdOrNull(booking3.id)!!.adhoc).isTrue()
  }

  private fun bookingIdListToCsvRows(rows: List<UUID>): String {
    val builder = CsvBuilder()
      .withUnquotedFields("adhoc_booking_id")
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it)
        .newRow()
    }

    return builder.build()
  }

}
