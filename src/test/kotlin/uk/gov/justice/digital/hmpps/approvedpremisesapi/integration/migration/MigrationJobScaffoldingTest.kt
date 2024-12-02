package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class MigrationJobScaffoldingTest : SeedTestBase() {
  @Test
  fun `Requesting a Seed operation returns 202`() {
    webTestClient.post()
      .uri("/migration-job")
      .bodyValue(
        MigrationJobRequest(
          jobType = MigrationJobType.ALL_USERS_FROM_COMMUNITY_API,
        ),
      )
      .exchange()
      .expectStatus()
      .isAccepted
  }
}
