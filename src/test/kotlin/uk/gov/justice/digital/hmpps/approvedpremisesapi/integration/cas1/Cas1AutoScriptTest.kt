package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1AutoScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class Cas1AutoScriptTest : IntegrationTestBase() {

  @Autowired
  lateinit var seedLogger: SeedLogger

  @Autowired
  lateinit var applicationService: ApplicationService

  @Autowired
  lateinit var userService: UserService

  @Autowired
  lateinit var offenderService: OffenderService

  @Test
  fun `ensure auto script runs`() {
    `Given a User` { user, _ ->
      `Given an Offender` { offenderDetails, _ ->

        APDeliusContext_mockSuccessfulTeamsManagingCaseCall(
          offenderDetails.otherIds.crn,
          ManagingTeamsResponse(
            teamCodes = listOf("TEAM1"),
          ),
        )

        Cas1AutoScript(
          seedLogger,
          applicationService,
          userService,
          offenderService,
        ).script(
          deliusUserName = user.deliusUsername,
          crn = offenderDetails.otherIds.crn,
        )
      }
    }
  }
}
