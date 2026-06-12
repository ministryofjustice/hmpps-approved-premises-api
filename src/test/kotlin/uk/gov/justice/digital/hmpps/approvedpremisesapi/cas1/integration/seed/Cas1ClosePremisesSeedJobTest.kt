package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Cas1ClosePremisesSeedJobTest : SeedTestBase() {

  @Test
  fun `Closing a premises updates all beds, active out-of-service beds and disables new space bookings`() {
    val premises = givenAnApprovedPremises()
    val otherPremises = givenAnApprovedPremises()

    val bed1 = givenAnApprovedPremisesBed(premises = premises)
    val bed2 = givenAnApprovedPremisesBed(premises = premises)
    val otherBed = givenAnApprovedPremisesBed(premises = otherPremises)

    val oosb1 = givenAnOutOfServiceBed(bed = bed1)
    val otherOosb = givenAnOutOfServiceBed(bed = otherBed)

    val closureDate = LocalDate.now().plusMonths(6)

    seed(
      SeedFileType.approvedPremisesClosePremises,
      rowsToCsv(
        listOf(
          SiteClosureCsvRow(
            premisesId = premises.id.toString(),
            closureDate = closureDate.toString(),
            notes = "Closing premises",
          ),
        ),
      ),
    )

    val updatedPremises = approvedPremisesRepository.findById(premises.id).get()
    assertThat(updatedPremises.allowNewSpaceBookings).isFalse()

    val updatedBed1 = bedRepository.findById(bed1.id).get()
    assertThat(updatedBed1.endDate).isEqualTo(closureDate)

    val updatedBed2 = bedRepository.findById(bed2.id).get()
    assertThat(updatedBed2.endDate).isEqualTo(closureDate)

    val updatedOosb1 = cas1OutOfServiceBedTestRepository.findById(oosb1.id).get()
    assertThat(updatedOosb1.endDate).isEqualTo(closureDate)

    val otherPremisesAfter = approvedPremisesRepository.findById(otherPremises.id).get()
    assertThat(otherPremisesAfter.allowNewSpaceBookings).isTrue()

    val otherBedAfter = bedRepository.findById(otherBed.id).get()
    assertThat(otherBedAfter.endDate).isNull()

    val otherOosbAfter = cas1OutOfServiceBedTestRepository.findById(otherOosb.id).get()
    assertThat(otherOosbAfter.endDate).isNotEqualTo(closureDate)
  }

  private fun rowsToCsv(rows: List<SiteClosureCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "premises_id",
        "closure_date",
        "notes",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.premisesId)
        .withQuotedField(it.closureDate)
        .withQuotedField(it.notes ?: "")
        .newRow()
    }

    return builder.build()
  }

  data class SiteClosureCsvRow(
    val premisesId: String,
    val closureDate: String,
    val notes: String?,
  )
}
