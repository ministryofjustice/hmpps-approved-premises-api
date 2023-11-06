package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.NomisUserRoles_mockSuccessfulNomisUserDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedNomisUsersTest : SeedTestBase() {
  @Test
  fun `Attempting to seed a real but currently unknown user succeeds`() {
    val nomisUserDetail = NomisUserDetailFactory()
      .withUsername("UNKNOWN-USER_GEN")
      .withFirstName("John")
      .withLastName("Smith")
      .withPrimaryEmail("john.smith@example.com")
      .withStaffId(6789)
      .produce()

    NomisUserRoles_mockSuccessfulNomisUserDetailCall(nomisUserDetail)

    withCsv(
      "unknown-nomis-user",
      nomisUserSeedCsvRowsToCsv(
        listOf(
          NomisUsersSeedCsvRowFactory()
            .withNomisUsername("UNKNOWN-USER_GEN")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.nomisUsers, "unknown-nomis-user")

    val persistedUser = nomisUserRepository.findByNomisUsername("UNKNOWN-USER_GEN")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.nomisStaffId).isEqualTo(6789)
    assertThat(persistedUser!!.name).isEqualTo("John Smith")
  }

  private fun nomisUserSeedCsvRowsToCsv(rows: List<NomisUsersSeedUntypedEnumsCsvRow>):
    String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "nomisUsername",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.nomisUsername)
        .newRow()
    }

    return builder.build()
  }
}

data class NomisUsersSeedUntypedEnumsCsvRow(
  val nomisUsername: String,
)

class NomisUsersSeedCsvRowFactory : Factory<NomisUsersSeedUntypedEnumsCsvRow> {
  private var nomisUsername: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withNomisUsername(nomisUsername: String) = apply {
    this.nomisUsername = { nomisUsername }
  }

  override fun produce() = NomisUsersSeedUntypedEnumsCsvRow(
    nomisUsername = this.nomisUsername(),
  )
}
