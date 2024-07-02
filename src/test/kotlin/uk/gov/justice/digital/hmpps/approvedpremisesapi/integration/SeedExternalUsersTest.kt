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
class SeedExternalUsersTest : SeedTestBase() {
  @Test
  fun `Attempting to seed a fake currently unknown user succeeds`() {
    externalUserRepository.deleteAll()

    withCsv(
      "unknown-external-user",
      externalUserSeedCsvRowsToCsv(
        listOf(
          ExternalUsersSeedCsvRowFactory()
            .withUsername("CAS2_ASSESSOR_FAKE")
            .withName("Chas Ash")
            .withEmail("chas.ash@example.com")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.externalUsers, "unknown-external-user")

    val persistedUser = externalUserRepository.findByUsername("CAS2_ASSESSOR_FAKE")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.name).isEqualTo("Chas Ash")
    assertThat(persistedUser.email).isEqualTo("chas.ash@example.com")
  }

  @Test
  fun `Attempting to seed a fake user whose username exists does nothing`() {
    externalUserRepository.deleteAll()

    externalUserEntityFactory.produceAndPersist {
      withUsername("CAS2_ASSESSOR_FAKE")
      withName("Chas Ash")
      withEmail("chas.ash@example.com")
    }

    withCsv(
      "existing-external-user",
      externalUserSeedCsvRowsToCsv(
        listOf(
          ExternalUsersSeedCsvRowFactory()
            .withUsername("CAS2_ASSESSOR_FAKE")
            .withName("Cas New Ash")
            .withEmail("cas.new.ash@example.com")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.externalUsers, "existing-external-user")

    val persistedUser = externalUserRepository.findByUsername("CAS2_ASSESSOR_FAKE")

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.name).isNotEqualTo("Chas New Ash")
    assertThat(persistedUser!!.email).isNotEqualTo("chas.new.ash@example.com")
  }

  private fun externalUserSeedCsvRowsToCsv(rows: List<ExternalUsersSeedUntypedEnumsCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "username",
        "name",
        "email",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.username)
        .withQuotedField(it.name)
        .withQuotedField(it.email)
        .newRow()
    }

    return builder.build()
  }
}

data class ExternalUsersSeedUntypedEnumsCsvRow(
  val username: String,
  val name: String,
  val email: String,
)

class ExternalUsersSeedCsvRowFactory : Factory<ExternalUsersSeedUntypedEnumsCsvRow> {
  private var username: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var email: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withUsername(externalUsername: String) = apply {
    this.username = { externalUsername }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withEmail(email: String) = apply {
    this.email = { email }
  }

  override fun produce() = ExternalUsersSeedUntypedEnumsCsvRow(
    username = this.username(),
    name = this.name(),
    email = this.email(),
  )
}
