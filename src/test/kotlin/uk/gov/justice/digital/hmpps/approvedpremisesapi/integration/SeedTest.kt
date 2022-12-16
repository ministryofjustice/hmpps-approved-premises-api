package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedRequest

class SeedTest : IntegrationTestBase() {
  @Test
  fun `Requesting a Seed operation returns 202`() {
    webTestClient.post()
      .uri("/seed")
      .bodyValue(
        SeedRequest(
          seedType = SeedFileType.approvedPremises,
          fileName = "file.csv"
        )
      )
      .exchange()
      .expectStatus()
      .isAccepted
  }
}
