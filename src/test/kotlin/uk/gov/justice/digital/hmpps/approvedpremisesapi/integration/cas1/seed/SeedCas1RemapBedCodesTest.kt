package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1RemapBedCodesSeedCsvRow

class SeedCas1RemapBedCodesTest : SeedTestBase() {

  @Test
  fun `unknown bed code returns error`() {
    givenAnApprovedPremises(name = "the premises name")

    seed(
      SeedFileType.approvedPremisesRemapBedCodes,
      rowsToCsv(
        listOf(
          Cas1RemapBedCodesSeedCsvRow(premisesName = "the premises name", oldBedCode = "OLDCODE1", newBedCode = "NEWCODE1"),
        ),
      ),
    )

    assertError(
      row = 1,
      message = "No bed found for code 'OLDCODE1'",
    )
  }

  @Test
  fun `unexpected premises name returns error`() {
    val premises = givenAnApprovedPremises(name = "wrong premise name")
    givenAnApprovedPremisesBed(
      premises = premises,
      bedCode = "OLDCODE1",
    )

    seed(
      SeedFileType.approvedPremisesRemapBedCodes,
      rowsToCsv(
        listOf(
          Cas1RemapBedCodesSeedCsvRow(premisesName = "the premise name", oldBedCode = "OLDCODE1", newBedCode = "NEWCODE1"),
        ),
      ),
    )

    assertError(
      row = 1,
      message = "Expected premises with name 'the premise name' for bed 'OLDCODE1' but found 'wrong premise name'",
    )
  }

  @Test
  fun success() {
    val premises = givenAnApprovedPremises(name = "the premise name")
    val bed = givenAnApprovedPremisesBed(
      premises = premises,
      bedCode = "OLDCODE1",
    )

    seed(
      SeedFileType.approvedPremisesRemapBedCodes,
      rowsToCsv(
        listOf(
          Cas1RemapBedCodesSeedCsvRow(premisesName = "the premise name", oldBedCode = "OLDCODE1", newBedCode = "NEWCODE1"),
        ),
      ),
    )

    val updatedBed = bedRepository.findByCode("NEWCODE1")
    assertThat(updatedBed).isNotNull()
    assertThat(updatedBed!!.id).isEqualTo(bed.id)
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
