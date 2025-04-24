package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateActualArrivalDateSeedJobCsvRow
import java.time.LocalDate

class SeedCas1UpdateActualArrivalDateTest : SeedTestBase() {

  @Test
  fun success() {
    val spaceBooking = givenACas1SpaceBooking(
      crn = "CRN1",
      deliusEventNumber = "101",
      actualArrivalDate = LocalDate.of(2025, 5, 1),
      canonicalArrivalDate = LocalDate.of(2025, 5, 1),
      premises = givenAnApprovedPremises(name = "My Test Premise"),
    )

    seed(
      SeedFileType.approvedPremisesUpdateActualArrivalDate,
      rowsToCsv(
        listOf(
          Cas1UpdateActualArrivalDateSeedJobCsvRow(
            spaceBookingId = spaceBooking.id,
            currentArrivalDate = LocalDate.of(2025, 5, 1),
            updatedArrivalDate = LocalDate.of(2025, 7, 1),
          ),
        ),
      ),
    )

    val updatedSpaceBooking = cas1SpaceBookingRepository.findById(spaceBooking.id).get()
    assertThat(updatedSpaceBooking.actualArrivalDate).isEqualTo(LocalDate.of(2025, 7, 1))
    assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(LocalDate.of(2025, 7, 1))

    val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(updatedSpaceBooking.application!!.id)
    assertThat(notes).hasSize(1)
    assertThat(notes)
      .extracting("body")
      .contains(
        "Actual arrival date for booking at 'My Test Premise' has been updated from 2025-05-01 to 2025-07-01 by application support",
      )
  }

  private fun rowsToCsv(rows: List<Cas1UpdateActualArrivalDateSeedJobCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "space_booking_id",
        "current_arrival_date",
        "updated_arrival_date",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.spaceBookingId.toString())
        .withQuotedField(it.currentArrivalDate.toString())
        .withQuotedField(it.updatedArrivalDate.toString())
        .newRow()
    }

    return builder.build()
  }
}
