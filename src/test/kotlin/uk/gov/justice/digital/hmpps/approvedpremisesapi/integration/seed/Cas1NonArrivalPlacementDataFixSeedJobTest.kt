package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import java.time.Instant
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Cas1NonArrivalPlacementDataFixSeedJobTest : SeedTestBase() {

  @Test
  fun `Clear non-arrival data for a Space Booking`() {
    val nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist()

    val bookingToFix = givenACas1SpaceBooking(
      crn = "CRN123",
      nonArrivalConfirmedAt = Instant.now(),
      nonArrivalReason = nonArrivalReason,
      nonArrivalNotes = "Some notes",
    )

    val otherBooking = givenACas1SpaceBooking(
      crn = "CRN456",
      nonArrivalConfirmedAt = Instant.now(),
      nonArrivalReason = nonArrivalReason,
      nonArrivalNotes = "Other notes",
    )

    val bookingWithNoNonArrival = givenACas1SpaceBooking(
      crn = "CRN789",
      nonArrivalConfirmedAt = null,
    )

    seed(
      SeedFileType.approvedPremisesRemovePlacementNonArrivalData,
      rowsToCsv(
        listOf(
          PlacementSeedRow(
            crn = "CRN123",
            spaceBookingId = bookingToFix.id,
          ),
        ),
      ),
    )

    val updatedBookingToFix = cas1SpaceBookingRepository.findById(bookingToFix.id).get()
    assertThat(updatedBookingToFix.nonArrivalConfirmedAt).isNull()
    assertThat(updatedBookingToFix.nonArrivalNotes).isNull()
    assertThat(updatedBookingToFix.nonArrivalReason).isNull()

    val updatedOtherBooking = cas1SpaceBookingRepository.findById(otherBooking.id).get()
    assertThat(updatedOtherBooking.nonArrivalConfirmedAt).isNotNull()
    assertThat(updatedOtherBooking.nonArrivalNotes).isEqualTo("Other notes")
    assertThat(updatedOtherBooking.nonArrivalReason).isEqualTo(nonArrivalReason)

    val updatedBookingWithNoNonArrival = cas1SpaceBookingRepository.findById(bookingWithNoNonArrival.id).get()
    assertThat(updatedBookingWithNoNonArrival.nonArrivalConfirmedAt).isNull()
  }

  @Test
  fun `Seed Job throws exception if CRN does not match`() {
    val booking = givenACas1SpaceBooking(
      crn = "CRN123",
      nonArrivalConfirmedAt = Instant.now(),
    )

    seed(
      SeedFileType.approvedPremisesRemovePlacementNonArrivalData,
      rowsToCsv(
        listOf(
          PlacementSeedRow(
            crn = "WRONGCRN",
            spaceBookingId = booking.id,
          ),
        ),
      ),
    )

    assertError(1, "Placement with ID ${booking.id} has incorrect CRN CRN123")
  }

  private fun rowsToCsv(rows: List<PlacementSeedRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "crn",
        "space_booking_id",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.crn)
        .withQuotedField(it.spaceBookingId.toString())
        .newRow()
    }

    return builder.build()
  }

  data class PlacementSeedRow(val crn: String, val spaceBookingId: UUID)
}
