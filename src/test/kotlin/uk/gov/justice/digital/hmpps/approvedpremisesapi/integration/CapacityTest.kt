package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.DateCapacity
import java.time.LocalDate

class CapacityTest : IntegrationTestBase() {
  @Test
  fun `Get Capacity without JWT returns 401`() {
    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    webTestClient.get()
      .uri("/premises/${premises.id}/capacity")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Capacity on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidClientCredentialsJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/capacity")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get Capacity with no bookings or lost beds returns OK with empty list body`() {
    val premises = premisesEntityFactory.produceAndPersist {
      withTotalBeds(30)
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }

    val jwt = jwtAuthHelper.createValidClientCredentialsJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/capacity")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<DateCapacity>()
      .hasSize(0)
  }

  @Test
  fun `Get Capacity with booking in past returns OK with empty list body`() {
    val premises = premisesEntityFactory.produceAndPersist {
      withTotalBeds(30)
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    bookingEntityFactory.produceAndPersist {
      withDepartureDate(LocalDate.now().minusDays(1))
        .withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
        .withPremises(premises)
    }

    val jwt = jwtAuthHelper.createValidClientCredentialsJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/capacity")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<DateCapacity>()
      .hasSize(0)
  }

  @Test
  fun `Get Capacity with booking in future returns OK with list entry for each day until the booking ends`() {
    val premises = premisesEntityFactory.produceAndPersist {
      withTotalBeds(30)
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    bookingEntityFactory.produceAndPersist {
      withArrivalDate(LocalDate.now().plusDays(4))
      withDepartureDate(LocalDate.now().plusDays(6))
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withPremises(premises)
    }

    val jwt = jwtAuthHelper.createValidClientCredentialsJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/capacity")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<DateCapacity>()
      .hasSize(6)
      .contains(
        DateCapacity(date = LocalDate.now(), availableBeds = 30),
        DateCapacity(date = LocalDate.now().plusDays(1), availableBeds = 30),
        DateCapacity(date = LocalDate.now().plusDays(2), availableBeds = 30),
        DateCapacity(date = LocalDate.now().plusDays(3), availableBeds = 30),
        DateCapacity(date = LocalDate.now().plusDays(4), availableBeds = 29),
        DateCapacity(date = LocalDate.now().plusDays(5), availableBeds = 29)
      )
  }
}
