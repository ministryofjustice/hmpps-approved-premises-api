package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1RemapBedCodesSeedCsvRow

class SeedCas1RemapBedCodesTest : SeedTestBase() {

  @Test
  fun `Unknown bed code returns error`() {
  }

  @Test
  fun `Unexpected premises name returns error`() {
  }

  @Test
  fun success() {
    val premises = givenAnApprovedPremises(name = "the premise name")
    val bed = givenAnApprovedPremisesBed(
      premises = premises,
      bedCode = "OLDCODE1",
    )

    withCsv(
      "invalid-ap-rooms-service-scope",
      rowsToCsv(
        listOf(
          Cas1RemapBedCodesSeedCsvRow(premisesName = "the premise name", oldBedCode = "OLDCODE1", newBedCode = "NEWCODE1"),
        ),
      ),
    )

    assertThat(bedRepository.findByCode("NEWCODE1")!!.id).isEqualTo(bed.id)
  }

  private fun rowsToCsv(rows: List<Cas1RemapBedCodesSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "premises_name",
        "old_bed_code",
        "new_bed_code",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.premisesName)
        .withQuotedField(it.oldBedCode)
        .withQuotedField(it.newBedCode)
        .newRow()
    }

    return builder.build()
  }
}
