package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import java.time.LocalDate
import java.util.UUID
class PremisesSummaryTest : IntegrationTestBase() {
  @Test
  fun `Get all CAS3 Premises returns OK with correct body`() {
    givenAUser { user, jwt ->
      val uuid = UUID.randomUUID()

      val expectedCas3Premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(user.probationRegion)
        withId(uuid)
        withAddressLine1("221 Baker Street")
        withAddressLine2("221B")
        withPostcode("NW1 6XE")
        withStatus(PropertyStatus.active)
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }
        }
        withService("CAS3")
      }

      // unexpectedCas3Premises that's in a different region
      temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { expectedCas3Premises }
      }

      bedEntityFactory.produceAndPersistMultiple(5) {
        withYieldedRoom { room }
      }

      webTestClient.get()
        .uri("/premises/summary")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$[0].id").isEqualTo(uuid.toString())
        .jsonPath("$[0].addressLine1").isEqualTo("221 Baker Street")
        .jsonPath("$[0].addressLine2").isEqualTo("221B")
        .jsonPath("$[0].postcode").isEqualTo("NW1 6XE")
        .jsonPath("$[0].status").isEqualTo("active")
        .jsonPath("$[0].pdu").isEqualTo(expectedCas3Premises.probationDeliveryUnit!!.name)
        .jsonPath("$[0].localAuthorityAreaName").isEqualTo(expectedCas3Premises.localAuthorityArea!!.name)
        .jsonPath("$[0].bedCount").isEqualTo(5)
        .jsonPath("$.length()").isEqualTo(1)
    }
  }

  @Test
  fun `Get all CAS3 Premises returns bedspace count as expected when there is an archived bedspace`() {
    givenAUser { user, jwt ->
      val uuid = UUID.randomUUID()

      val expectedCas3Premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(user.probationRegion)
        withId(uuid)
        withAddressLine1("221 Baker Street")
        withAddressLine2("221B")
        withPostcode("NW1 6XE")
        withStatus(PropertyStatus.active)
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }
        }
        withService("CAS3")
      }

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { expectedCas3Premises }
      }

      bedEntityFactory.produceAndPersistMultiple(3) {
        withYieldedRoom { room }
      }

      bedEntityFactory.produceAndPersistMultiple(1) {
        withYieldedRoom { room }
        withEndDate { LocalDate.now().minusWeeks(1) }
      }

      webTestClient.get()
        .uri("/premises/summary")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$[0].id").isEqualTo(uuid.toString())
        .jsonPath("$[0].addressLine1").isEqualTo("221 Baker Street")
        .jsonPath("$[0].addressLine2").isEqualTo("221B")
        .jsonPath("$[0].postcode").isEqualTo("NW1 6XE")
        .jsonPath("$[0].status").isEqualTo("active")
        .jsonPath("$[0].pdu").isEqualTo(expectedCas3Premises.probationDeliveryUnit!!.name)
        .jsonPath("$[0].bedCount").isEqualTo(3)
        .jsonPath("$.length()").isEqualTo(1)
    }
  }

  @Test
  fun `Get all CAS3 Premises returns a bedspace count as expected when beds are active`() {
    givenAUser { user, jwt ->
      val uuid = UUID.randomUUID()

      val expectedCas3Premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(user.probationRegion)
        withId(uuid)
        withAddressLine1("221 Baker Street")
        withAddressLine2("221B")
        withPostcode("NW1 6XE")
        withStatus(PropertyStatus.active)
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }
        }
        withService("CAS3")
      }

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { expectedCas3Premises }
      }

      bedEntityFactory.produceAndPersistMultiple(3) {
        withYieldedRoom { room }
      }

      bedEntityFactory.produceAndPersistMultiple(1) {
        withYieldedRoom { room }
        withEndDate { LocalDate.now().plusWeeks(1) }
      }

      webTestClient.get()
        .uri("/premises/summary")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$[0].id").isEqualTo(uuid.toString())
        .jsonPath("$[0].addressLine1").isEqualTo("221 Baker Street")
        .jsonPath("$[0].addressLine2").isEqualTo("221B")
        .jsonPath("$[0].postcode").isEqualTo("NW1 6XE")
        .jsonPath("$[0].status").isEqualTo("active")
        .jsonPath("$[0].pdu").isEqualTo(expectedCas3Premises.probationDeliveryUnit!!.name)
        .jsonPath("$[0].bedCount").isEqualTo(4)
        .jsonPath("$.length()").isEqualTo(1)
    }
  }

  @Test
  fun `Get all Premises throws error with incorrect service name`() {
    givenAUser { _, jwt ->
      webTestClient.get()
        .uri("/premises/summary")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "service-name")
        .exchange()
        .expectStatus()
        .is4xxClientError
    }
  }

  @Test
  fun `Get all CAS1 Premises returns OK with correct body`() {
    givenAUser { _, jwt ->
      val uuid = UUID.randomUUID()
      val apArea = apAreaEntityFactory.produceAndPersist()
      val probationRegion = probationRegionEntityFactory.produceAndPersist {
        withApArea(apArea)
      }

      val cas1Premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(probationRegion)
        withId(uuid)
        withAddressLine1("221 Baker Street")
        withAddressLine2("221B")
        withPostcode("NW1 6XE")
        withStatus(PropertyStatus.active)
        withApCode("APCODE")
        withService("CAS3")
      }

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { cas1Premises }
      }

      val liveBeds = listOf(
        bedEntityFactory.produceAndPersistMultiple(5) {
          withYieldedRoom { room }
        },
        // Beds scheduled for removal in the future
        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { room }
          withEndDate { LocalDate.now().plusDays(5) }
        },
      ).flatten()

      // Removed beds
      bedEntityFactory.produceAndPersistMultiple(2) {
        withYieldedRoom { room }
        withEndDate { LocalDate.now().minusDays(5) }
      }

      // Beds scheduled for removal today
      bedEntityFactory.produceAndPersistMultiple(3) {
        withYieldedRoom { room }
        withEndDate { LocalDate.now() }
      }

      webTestClient.get()
        .uri("/premises/summary")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$[0].id").isEqualTo(uuid.toString())
        .jsonPath("$[0].addressLine1").isEqualTo("221 Baker Street")
        .jsonPath("$[0].addressLine2").isEqualTo("221B")
        .jsonPath("$[0].postcode").isEqualTo("NW1 6XE")
        .jsonPath("$[0].status").isEqualTo("active")
        .jsonPath("$[0].apCode").isEqualTo("APCODE")
        .jsonPath("$[0].bedCount").isEqualTo(liveBeds.count())
        .jsonPath("$[0].probationRegion").isEqualTo(probationRegion.name)
        .jsonPath("$[0].apArea").isEqualTo(apArea.name)
    }
  }

  @Test
  fun `Get all CAS1 Premises filters by probation region`() {
    givenAUser { _, jwt ->
      val region1 = probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      val region2 = probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }

      val region1Premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(region1)
      }

      approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(region2)
      }

      webTestClient.get()
        .uri("/premises/summary?probationRegionId=${region1.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].id").isEqualTo(region1Premises.id.toString())
    }
  }

  @Test
  fun `Get all CAS1 Premises filters by AP Area`() {
    givenAUser { _, jwt ->
      val apArea = apAreaEntityFactory.produceAndPersist()
      val region1 = probationRegionEntityFactory.produceAndPersist { withApArea(apArea) }
      val region2 = probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }

      val apAreaPremises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(region1)
      }

      approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(region2)
      }

      webTestClient.get()
        .uri("/premises/summary?apAreaId=${apArea.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].id").isEqualTo(apAreaPremises.id.toString())
    }
  }
}
