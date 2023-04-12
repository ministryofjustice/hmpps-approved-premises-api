package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesSummaryTransformer
import java.util.UUID
class PremisesSummaryTest : IntegrationTestBase() {
  lateinit var premisesSummaryTransformer: PremisesSummaryTransformer

  @Test
  fun `Get all CAS3 Premises returns OK with correct body`() {
    `Given a User` { _, jwt ->
      val uuid = UUID.randomUUID()

      val cas3Premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withId(uuid)
        withAddressLine1("221 Baker Street")
        withAddressLine2("221B")
        withPostcode("NW1 6XE")
        withStatus(PropertyStatus.active)
        withPdu("PDU")
        withService("CAS3")
        withTotalBeds(0) // A static legacy column that we don't use
      }

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { cas3Premises }
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
        .jsonPath("$[0].pdu").isEqualTo("PDU")
        .jsonPath("$[0].bedCount").isEqualTo(5)
    }
  }

  @Test
  fun `Get all CAS1 Premises returns OK with correct body`() {
    `Given a User` { _, jwt ->
      val uuid = UUID.randomUUID()

      val cas3Premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withId(uuid)
        withAddressLine1("221 Baker Street")
        withAddressLine2("221B")
        withPostcode("NW1 6XE")
        withStatus(PropertyStatus.active)
        withApCode("APCODE")
        withService("CAS3")
        withTotalBeds(0) // A static legacy column that we don't use
      }

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { cas3Premises }
      }

      bedEntityFactory.produceAndPersistMultiple(5) {
        withYieldedRoom { room }
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
        .jsonPath("$[0].bedCount").isEqualTo(5)
    }
  }
}
