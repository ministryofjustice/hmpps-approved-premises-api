package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedUpdateUsersFromApiTest : SeedTestBase() {

  companion object {
    const val USERNAME: String = "KNOWN-USERNAME"
  }

  @Test
  fun `Update existing user`() {
    userEntityFactory.produceAndPersist {
      withDeliusUsername(USERNAME)
      withEmail("oldemail@localhost")
      withYieldedProbationRegion { `Given a Probation Region`() }
    }

    val staffUserDetails = StaffDetailFactory.staffDetail(deliusUsername = USERNAME, email = "updatedemail@localhost")

    ApDeliusContext_addStaffDetailResponse(staffUserDetails)

    val csv = CsvBuilder()
      .withUnquotedFields(
        "delius_username",
        "service_name",
      )
      .newRow()
      .withQuotedField(USERNAME)
      .withQuotedField(ServiceName.approvedPremises)
      .newRow()
      .build()

    withCsv("known-user-csv", csv)

    seedService.seedData(SeedFileType.updateUsersFromApi, "known-user-csv.csv")

    val persistedUser = userRepository.findByDeliusUsername(USERNAME)

    assertThat(persistedUser).isNotNull
    assertThat(persistedUser!!.email).isEqualTo("updatedemail@localhost")
  }
}
