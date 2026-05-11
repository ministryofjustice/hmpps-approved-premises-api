package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed.Cas3UpdatePostcodeRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import java.util.UUID

class Cas3UpdatePremisesPostcodeSeedJobTest : SeedTestBase() {
  @Test
  fun `successfully updates a postcode for a cas3 premises`() {
    val cas3PremisesId = UUID.randomUUID()
    val postcode = "SOME TEST POSTCODE (!)"
    givenACas3Premises(id = cas3PremisesId, postCode = postcode)

    val contents = rowsToCsv(listOf(Cas3UpdatePostcodeRow(cas3PremisesId = cas3PremisesId, postcode = "AA11 223")))

    seed(SeedFileType.cas3UpdatePremisesPostcode, contents)

    val updated = cas3PremisesRepository.findByIdOrNull(cas3PremisesId)!!
    assertThat(updated.postcode).isEqualTo("AA11 223")
  }

  private fun rowsToCsv(rows: List<Cas3UpdatePostcodeRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields("cas3PremisesId", "postcode")
      .newRow()

    rows.forEach {
      builder
        .withQuotedFields(it.cas3PremisesId, it.postcode)
        .newRow()
    }

    return builder.build()
  }
}
