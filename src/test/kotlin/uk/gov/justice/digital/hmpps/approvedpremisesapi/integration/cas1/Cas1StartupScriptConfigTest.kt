package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1StartupScript

class Cas1StartupScriptConfigTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas1StartupScript: Cas1StartupScript

  @Test
  fun `ensure local auto script runs`() {
    givenAnOffender { offenderDetails, _ ->

      apDeliusContextMockSuccessfulTeamsManagingCaseCall(
        offenderDetails.otherIds.crn,
        ManagingTeamsResponse(
          teamCodes = listOf("TEAM1"),
        ),
      )

      cas1StartupScript.scriptLocal()
    }
  }

  @Test
  fun `ensure dev auto script runs`() {
    cas1StartupScript.scriptDev()
  }
}
