package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import java.util.UUID

class PremisesTest : IntegrationTestBase() {
  @Test
  fun `Get all Premises returns OK with correct body`() {
    val premises = premisesEntityFactory.configure {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.configure { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }.produceAndPersist()
      }
    }.produceAndPersistMultiple(10)

    val expectedJson = objectMapper.writeValueAsString(premises.map(::premisesEntityToExpectedApiResponse))

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Premises by ID returns OK with correct body`() {
    val premises = premisesEntityFactory.configure {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.configure { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }.produceAndPersist()
      }
    }.produceAndPersistMultiple(5)

    val premisesToGet = premises[2]
    val expectedJson = objectMapper.writeValueAsString(premisesEntityToExpectedApiResponse(premises[2]))

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises/${premisesToGet.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Premises by ID returns Not Found with correct body`() {
    val idToRequest = UUID.randomUUID().toString()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises/$idToRequest")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectHeader().contentType("application/problem+json")
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("title").isEqualTo("Not Found")
      .jsonPath("status").isEqualTo(404)
      .jsonPath("detail").isEqualTo("No Premises with an ID of $idToRequest could be found")
  }

  private fun premisesEntityToExpectedApiResponse(premises: PremisesEntity) = Premises(
    id = premises.id,
    name = premises.name,
    apCode = premises.apCode,
    postcode = premises.postcode,
    bedCount = premises.totalBeds,
    probationRegion = ProbationRegion(id = premises.probationRegion.id, name = premises.probationRegion.name),
    apArea = ApArea(id = premises.probationRegion.apArea.id, name = premises.probationRegion.apArea.name, identifier = premises.probationRegion.apArea.identifier),
    localAuthorityArea = LocalAuthorityArea(id = premises.localAuthorityArea.id, identifier = premises.localAuthorityArea.identifier, name = premises.localAuthorityArea.name)
  )
}
