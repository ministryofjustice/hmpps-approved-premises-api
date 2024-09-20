package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import java.time.LocalDate
import java.util.UUID

class BedSummaryTest : InitialiseDatabasePerClassTestBase() {
  lateinit var premises: PremisesEntity

  @BeforeEach
  fun setup() {
    val probationRegion = `Given a Probation Region`()

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    this.premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
    }
  }

  @Test
  fun `Getting beds for a premises without JWT returns 401`() {
    webTestClient.get()
      .uri("/premises/${premises.id}/beds")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting beds for a premises that does not exist returns 404`() {
    `Given a User` { _, jwt ->
      webTestClient.get()
        .uri("/premises/${UUID.randomUUID()}/beds")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "approved-premises")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting beds for a premises returns a list of beds`() {
    `Given a User` { _, jwt ->
      val bedWithoutBooking = bedEntityFactory.produceAndPersist {
        withRoom(
          roomEntityFactory.produceAndPersist {
            withPremises(premises)
          },
        )
      }

      val bedWithBooking = bedEntityFactory.produceAndPersist {
        withRoom(
          roomEntityFactory.produceAndPersist {
            withPremises(premises)
          },
        )
      }

      val bedWithLostBed = bedEntityFactory.produceAndPersist {
        withRoom(
          roomEntityFactory.produceAndPersist {
            withPremises(premises)
          },
        )
      }

      bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bedWithBooking)
        withArrivalDate(LocalDate.now().minusDays((7).toLong()))
        withDepartureDate(LocalDate.now().plusDays((20).toLong()))
      }

      lostBedsEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bedWithLostBed)
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withStartDate(LocalDate.now().minusDays((7).toLong()))
        withEndDate(LocalDate.now().plusDays((20).toLong()))
      }

      val expectedResponse = listOf(
        BedSummary(bedWithoutBooking.id, bedWithoutBooking.name, bedWithoutBooking.room.name, BedStatus.available),
        BedSummary(bedWithBooking.id, bedWithBooking.name, bedWithBooking.room.name, BedStatus.occupied),
        BedSummary(bedWithLostBed.id, bedWithLostBed.name, bedWithLostBed.room.name, BedStatus.outOfService),
      )

      webTestClient.get()
        .uri("/premises/${premises.id}/beds")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "approved-premises")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(objectMapper.writeValueAsString(expectedResponse))
    }
  }
}
