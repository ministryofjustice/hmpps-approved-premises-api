package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.LocalDate
import java.util.UUID

class CapacityTest : IntegrationTestBase() {
  @Test
  fun `Get Capacity without JWT returns 401`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
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
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/capacity")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `Get Capacity with no bookings or lost beds on Approved Premises returns OK with empty list body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
      }

      bedEntityFactory.produceAndPersistMultiple(30) {
        withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
      }

      webTestClient.get()
        .uri("/premises/${premises.id}/capacity")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<DateCapacity>()
        .hasSize(0)
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `Get Capacity for Approved Premises with booking in past returns OK with empty list body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      bedEntityFactory.produceAndPersistMultiple(30) {
        withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
      }

      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)

      bookingEntityFactory.produceAndPersist {
        withDepartureDate(LocalDate.now().minusDays(1))
          .withStaffKeyWorkerCode(keyWorker.code)
          .withPremises(premises)
      }

      webTestClient.get()
        .uri("/premises/${premises.id}/capacity")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<DateCapacity>()
        .hasSize(0)
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `Get Capacity for Approved Premises with booking in future returns OK with list entry for each day until the booking ends when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      bedEntityFactory.produceAndPersistMultiple(30) {
        withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
      }

      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)

      bookingEntityFactory.produceAndPersist {
        withArrivalDate(LocalDate.now().plusDays(4))
        withDepartureDate(LocalDate.now().plusDays(6))
        withStaffKeyWorkerCode(keyWorker.code)
        withPremises(premises)
      }

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
          DateCapacity(date = LocalDate.now().plusDays(5), availableBeds = 29),
        )
    }
  }

  @Test
  fun `Get Capacity on a Temporary Accommodation Premises that's not in the user's region returns 403 Forbidden`() {
    `Given a User` { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
      }

      webTestClient.get()
        .uri("/premises/${premises.id}/capacity")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }
}
