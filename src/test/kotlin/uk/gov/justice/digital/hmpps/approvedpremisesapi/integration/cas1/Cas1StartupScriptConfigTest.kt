package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1StartupScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

class Cas1StartupScriptConfigTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas1StartupScript: Cas1StartupScript

  @Test
  fun `ensure dev auto script runs`() {
    cas1StartupScript.createDevApplications()
  }
}
