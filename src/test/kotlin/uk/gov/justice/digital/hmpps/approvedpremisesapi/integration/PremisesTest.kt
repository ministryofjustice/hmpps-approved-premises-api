package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import java.time.LocalDate
import java.util.UUID

class PremisesTest : IntegrationTestBase() {
  @Autowired
  lateinit var premisesTransformer: PremisesTransformer

  @Test
  fun `Get all Premises returns OK with correct body`() {
    val premises = premisesEntityFactory.produceAndPersistMultiple(10) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withTotalBeds(20)
    }

    val expectedJson = objectMapper.writeValueAsString(
      premises.map {
        premisesTransformer.transformJpaToApi(it, 20)
      }
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
    val premises = premisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withTotalBeds(20)
    }

    val premisesToGet = premises[2]
    val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20))

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
  fun `Get Premises by ID returns OK with correct body when capacity is used`() {
    val premises = premisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withTotalBeds(20)
    }

    val keyWorker = StaffMemberFactory().produce()
    mockStaffMemberCommunityApiCall(keyWorker)

    bookingEntityFactory.produceAndPersist {
      withPremises(premises[2])
      withArrivalDate(LocalDate.now().minusDays(2))
      withDepartureDate(LocalDate.now().plusDays(4))
      withStaffKeyWorkerId(keyWorker.staffIdentifier)
    }

    val premisesToGet = premises[2]
    val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 19))

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
}
