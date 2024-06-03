package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedNomisUsersTest : SeedTestBase() {
  @Test
  fun `Attempting to seed a fake currently unknown user succeeds`() {
    nomisUserRepository.deleteAll()

    withCsv(
      "unknown-nomis-user",
      nomisUserSeedCsvRowsToCsv(
        listOf(
          NomisUsersSeedCsvRowFactory()
            .withNomisUsername("ROGER_SMITH_FAKE")
            .withName("Roger Smith")
            .withEmail("roger.smith@example.com")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.nomisUsers, "unknown-nomis-user")

    val persistedUser = nomisUserRepository.findByNomisUsername("ROGER_SMITH_FAKE")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.name).isEqualTo("Roger Smith")
    assertThat(persistedUser!!.email).isEqualTo("roger.smith@example.com")
  }

  @Test
  fun `Attempting to seed a fake user whose username exists does nothing`() {
    nomisUserRepository.deleteAll()

    nomisUserEntityFactory.produceAndPersist {
      withNomisUsername("ROGER_SMITH_FAKE")
      withName("Roger Smith")
      withEmail("roger.smith@example.com")
    }

    withCsv(
      "existing-nomis-user",
      nomisUserSeedCsvRowsToCsv(
        listOf(
          NomisUsersSeedCsvRowFactory()
            .withNomisUsername("ROGER_SMITH_FAKE")
            .withName("Roger New Smith")
            .withEmail("roger.new.smith@example.com")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.nomisUsers, "existing-nomis-user")

    val persistedUser = nomisUserRepository.findByNomisUsername("ROGER_SMITH_FAKE")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.name).isNotEqualTo("Roger New Smith")
    assertThat(persistedUser.email).isNotEqualTo("roger.new.smith@example.com")
  }

  private fun nomisUserSeedCsvRowsToCsv(rows: List<NomisUsersSeedUntypedEnumsCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "nomisUsername",
        "name",
        "email",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.nomisUsername)
        .withQuotedField(it.name)
        .withQuotedField(it.email)
        .newRow()
    }

    return builder.build()
  }
}

data class NomisUsersSeedUntypedEnumsCsvRow(
  val nomisUsername: String,
  val name: String,
  val email: String,
)

class NomisUsersSeedCsvRowFactory : Factory<NomisUsersSeedUntypedEnumsCsvRow> {
  private var nomisUsername: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var email: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withNomisUsername(nomisUsername: String) = apply {
    this.nomisUsername = { nomisUsername }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withEmail(email: String) = apply {
    this.email = { email }
  }

  override fun produce() = NomisUsersSeedUntypedEnumsCsvRow(
    nomisUsername = this.nomisUsername(),
    name = this.name(),
    email = this.email(),
  )
}
