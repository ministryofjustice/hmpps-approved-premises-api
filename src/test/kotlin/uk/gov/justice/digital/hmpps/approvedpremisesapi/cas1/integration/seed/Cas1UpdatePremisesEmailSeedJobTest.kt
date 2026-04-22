package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Cas1UpdatePremisesEmailSeedJobTest : SeedTestBase() {

  @Test
  fun `Returns success when email address is updated for a valid premises`() {
    val premises1 = givenAnApprovedPremises(emailAddress = "premises1@example.com")
    val premises2 = givenAnApprovedPremises(emailAddress = "premises2before@example.com")
    val premises3 = givenAnApprovedPremises(emailAddress = "premises3before@example.com")

    seed(
      SeedFileType.approvedPremisesUpdatePremisesEmail,
      rowsToCsv(
        listOf(
          PremisesEmailSeedRow(
            premisesId = premises2.id,
            emailAddress = "premises2after@example.com",
          ),
          PremisesEmailSeedRow(
            premisesId = premises3.id,
            emailAddress = "premises3after@example.com",
          ),
        ),
      ),
    )

    val premises1After = approvedPremisesRepository.findByQCode(premises1.qCode)
    assertThat(premises1After!!.emailAddress).isEqualTo("premises1@example.com")

    val premises2After = approvedPremisesRepository.findByQCode(premises2.qCode)
    assertThat(premises2After!!.emailAddress).isEqualTo("premises2after@example.com")

    val premises3After = approvedPremisesRepository.findByQCode(premises3.qCode)
    assertThat(premises3After!!.emailAddress).isEqualTo("premises3after@example.com")
  }

  @Test
  fun `Returns failure when email address is updated for a premises which doesn't exist`() {
    val randomUUID = UUID.randomUUID()

    seed(
      SeedFileType.approvedPremisesUpdatePremisesEmail,
      rowsToCsv(
        listOf(
          PremisesEmailSeedRow(
            premisesId = randomUUID,
            emailAddress = "after@example.com",
          ),
        ),
      ),
    )
    assertError(1, "Premises not found for premises ID $randomUUID")
  }

  private fun rowsToCsv(rows: List<PremisesEmailSeedRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "premises_id",
        "email_address",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.premisesId.toString())
        .withQuotedField(it.emailAddress)
        .newRow()
    }

    return builder.build()
  }

  data class PremisesEmailSeedRow(val premisesId: UUID, val emailAddress: String)
}
