package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed.Cas3UpdateSuppressArchiveLimitsRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import java.util.UUID

class Cas3UpdatePremisesSuppressArchiveLimitsSeedJobTest : SeedTestBase() {
  @Test
  fun `successfully sets and clears suppressArchiveLimits for cas3 premises`() {
    val premisesToSuppress = UUID.randomUUID()
    val premisesToClear = UUID.randomUUID()
    givenACas3Premises(id = premisesToSuppress)
    givenACas3Premises(id = premisesToClear)

    seed(
      SeedFileType.cas3UpdatePremisesSuppressArchiveLimits,
      rowsToCsv(listOf(Cas3UpdateSuppressArchiveLimitsRow(premisesToClear, true))),
    )
    assertThat(cas3PremisesRepository.findByIdOrNull(premisesToClear)!!.suppressArchiveLimits).isTrue()

    seed(
      SeedFileType.cas3UpdatePremisesSuppressArchiveLimits,
      rowsToCsv(
        listOf(
          Cas3UpdateSuppressArchiveLimitsRow(premisesToSuppress, true),
          Cas3UpdateSuppressArchiveLimitsRow(premisesToClear, false),
        ),
      ),
    )

    assertThat(cas3PremisesRepository.findByIdOrNull(premisesToSuppress)!!.suppressArchiveLimits).isTrue()
    assertThat(cas3PremisesRepository.findByIdOrNull(premisesToClear)!!.suppressArchiveLimits).isFalse()
  }

  @Test
  fun `does not fail when the premises does not exist`() {
    seed(
      SeedFileType.cas3UpdatePremisesSuppressArchiveLimits,
      rowsToCsv(listOf(Cas3UpdateSuppressArchiveLimitsRow(UUID.randomUUID(), true))),
    )
  }

  private fun rowsToCsv(rows: List<Cas3UpdateSuppressArchiveLimitsRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields("cas3PremisesId", "suppressArchiveLimits")
      .newRow()

    rows.forEach {
      builder
        .withQuotedFields(it.cas3PremisesId, it.suppressArchiveLimits)
        .newRow()
    }

    return builder.build()
  }
}
