package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1ApplicationSeedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1CreateTestApplicationsSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder

class SeedCas1CreateTestApplicationSeedTest : SeedTestBase() {

  @Test
  fun `Create test application`() {
    val (user) = givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_CRU_MEMBER))
    val (offender) = givenAnOffender()

    val crn = offender.otherIds.crn

    apDeliusContextMockSuccessfulTeamsManagingCaseCall(crn, ManagingTeamsResponse(teamCodes = listOf("TEAM1")))
    apOASysContextMockSuccessfulNeedsDetailsCall(crn, NeedsDetailsFactory().produce())
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    val premises = givenAnApprovedPremises(supportsSpaceBookings = true)

    postCodeDistrictFactory.produceAndPersist()

    seed(
      SeedFileType.approvedPremisesCreateTestApplications,
      contents = listOf(
        Cas1CreateTestApplicationsSeedCsvRow(
          creatorUsername = user.deliusUsername,
          crn = crn,
          count = 1,
          state = Cas1ApplicationSeedService.ApplicationState.BOOKED,
          premisesQCode = premises.qCode,
        ),
      ).toCsv(),
    )

    val applications = approvedPremisesApplicationRepository.findAll()
    assertThat(applications).hasSize(1)
  }

  private fun List<Cas1CreateTestApplicationsSeedCsvRow>.toCsv(): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "creator_username",
        "crn",
        "count",
        "state",
        "premises_qcode",
      )
      .newRow()

    this.forEach {
      builder
        .withQuotedField(it.creatorUsername)
        .withQuotedField(it.crn)
        .withQuotedField(it.count)
        .withQuotedField(it.state)
        .withQuotedField(it.premisesQCode ?: "")
        .newRow()
    }

    return builder.build()
  }
}
