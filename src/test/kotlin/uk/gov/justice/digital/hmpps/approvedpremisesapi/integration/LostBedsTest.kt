package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.LostBedReasons
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import java.time.LocalDate

class LostBedsTest : IntegrationTestBase() {
  @Autowired
  lateinit var lostBedsTransformer: LostBedsTransformer

  @Test
  fun `Create Lost Beds without JWT returns 401`() {
    val premises = premisesEntityFactory
      .withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      .withYieldedProbationRegion { probationRegionEntityFactory.withYieldedApArea { apAreaEntityFactory.produceAndPersist() }.produceAndPersist() }
      .produceAndPersist()

    webTestClient.post()
      .uri("/premises/${premises.id}/lost-beds")
      .bodyValue(
        NewLostBed(
          startDate = LocalDate.parse("2022-08-15"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 1,
          reason = LostBedReasons.damaged,
          referenceNumber = "REF-123",
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Lost Beds on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewLostBed(
          startDate = LocalDate.parse("2022-08-15"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 1,
          reason = LostBedReasons.damaged,
          referenceNumber = "REF-123",
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Create Lost Beds with endDate before startDate returns Bad Request with correct body`() {
    val premises = premisesEntityFactory
      .withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      .withYieldedProbationRegion { probationRegionEntityFactory.withYieldedApArea { apAreaEntityFactory.produceAndPersist() }.produceAndPersist() }
      .produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${premises.id}/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewLostBed(
          startDate = LocalDate.parse("2022-08-19"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 1,
          reason = LostBedReasons.damaged,
          referenceNumber = "REF-123",
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".invalid-params[0]").isEqualTo(
        mapOf(
          "propertyName" to "endDate",
          "errorType" to "Cannot be before startDate"
        )
      )
  }

  @Test
  fun `Create Lost Beds with less than 1 bed returns Bad Request with correct body`() {
    val premises = premisesEntityFactory
      .withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      .withYieldedProbationRegion { probationRegionEntityFactory.withYieldedApArea { apAreaEntityFactory.produceAndPersist() }.produceAndPersist() }
      .produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${premises.id}/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewLostBed(
          startDate = LocalDate.parse("2022-08-17"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 0,
          reason = LostBedReasons.damaged,
          referenceNumber = "REF-123",
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".invalid-params[0]").isEqualTo(
        mapOf(
          "propertyName" to "numberOfBeds",
          "errorType" to "Must be greater than 0"
        )
      )
  }

  @Test
  fun `Create Lost Beds returns OK with correct body`() {
    val premises = premisesEntityFactory
      .withTotalBeds(3)
      .withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      .withYieldedProbationRegion { probationRegionEntityFactory.withYieldedApArea { apAreaEntityFactory.produceAndPersist() }.produceAndPersist() }
      .produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${premises.id}/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewLostBed(
          startDate = LocalDate.parse("2022-08-17"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 3,
          reason = LostBedReasons.damaged,
          referenceNumber = "REF-123",
          notes = "notes"
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(".startDate").isEqualTo("2022-08-17")
      .jsonPath(".endDate").isEqualTo("2022-08-18")
      .jsonPath(".numberOfBeds").isEqualTo(3)
      .jsonPath(".reason").isEqualTo("Damaged")
      .jsonPath(".referenceNumber").isEqualTo("REF-123")
      .jsonPath(".notes").isEqualTo("notes")
  }
}
