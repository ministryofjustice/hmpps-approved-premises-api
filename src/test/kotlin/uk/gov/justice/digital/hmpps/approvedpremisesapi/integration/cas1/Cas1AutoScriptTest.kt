package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1AutoScript

class Cas1AutoScriptTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas1AutoScript: Cas1AutoScript

  @Test
  fun `ensure local auto script runs`() {
    givenAnOffender { offenderDetails, _ ->

      apDeliusContextMockSuccessfulTeamsManagingCaseCall(
        offenderDetails.otherIds.crn,
        ManagingTeamsResponse(
          teamCodes = listOf("TEAM1"),
        ),
      )

      cas1AutoScript.scriptLocal()
    }
  }

  @Test
  fun `ensure dev auto script runs`() {
    cas1AutoScript.scriptDev()
  }
}
