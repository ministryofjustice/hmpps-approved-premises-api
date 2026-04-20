package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Cas1CancelOutOfServiceBedsByPremisesJobTest : SeedTestBase() {

  @Test
  fun `Cancels all active out-of-service beds for a valid premises`() {
    val premises = givenAnApprovedPremises()
    val bed1 = givenAnApprovedPremisesBed(premises = premises)
    val bed2 = givenAnApprovedPremisesBed(premises = premises)
    val otherPremises = givenAnApprovedPremises()
    val otherBed = givenAnApprovedPremisesBed(premises = otherPremises)

    val oosb1 = givenAnOutOfServiceBed(bed = bed1)
    val oosb2 = givenAnOutOfServiceBed(bed = bed2)
    val otherOosb = givenAnOutOfServiceBed(bed = otherBed)

    assertThat(cas1OutOfServiceBedTestRepository.findById(oosb1.id).get().cancellation).isNull()
    assertThat(cas1OutOfServiceBedTestRepository.findById(oosb2.id).get().cancellation).isNull()
    assertThat(cas1OutOfServiceBedTestRepository.findById(otherOosb.id).get().cancellation).isNull()

    seed(
      SeedFileType.approvedPremisesCancelOutOfServiceBeds,
      rowsToCsv(
        listOf(
          CancelOutOfServiceBedsByPremisesCsvRow(
            premisesId = premises.id,
            notes = "Cancelled via seed job",
          ),
        ),
      ),
    )

    val oosb1After = cas1OutOfServiceBedTestRepository.findById(oosb1.id).get()
    assertThat(oosb1After.cancellation).isNotNull
    assertThat(oosb1After.cancellation!!.notes).isEqualTo("Cancelled via seed job")

    val oosb2After = cas1OutOfServiceBedTestRepository.findById(oosb2.id).get()
    assertThat(oosb2After.cancellation).isNotNull
    assertThat(oosb2After.cancellation!!.notes).isEqualTo("Cancelled via seed job")

    val otherOosbAfter = cas1OutOfServiceBedTestRepository.findById(otherOosb.id).get()
    assertThat(otherOosbAfter.cancellation).isNull()
  }

  @Test
  fun `Returns error if premises does not exist`() {
    val randomId = UUID.randomUUID()
    seed(
      SeedFileType.approvedPremisesCancelOutOfServiceBeds,
      rowsToCsv(
        listOf(
          CancelOutOfServiceBedsByPremisesCsvRow(
            premisesId = randomId,
            notes = "Notes",
          ),
        ),
      ),
    )

    assertError(1, "No Premises with ID $randomId exists.")
  }

  private fun rowsToCsv(rows: List<CancelOutOfServiceBedsByPremisesCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "premisesId",
        "notes",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.premisesId.toString())
        .withQuotedField(it.notes ?: "")
        .newRow()
    }

    return builder.build()
  }

  data class CancelOutOfServiceBedsByPremisesCsvRow(val premisesId: UUID, val notes: String?)
}
