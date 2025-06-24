package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.util.UUID

class Cas1BedSummaryTest : InitialiseDatabasePerClassTestBase() {
  lateinit var premises: PremisesEntity

  @Autowired
  lateinit var cas1BedSummaryTransformer: Cas1BedSummaryTransformer

  @BeforeEach
  fun setup() {
    this.premises = givenAnApprovedPremises()
  }

  @Test
  fun `Getting beds for a premises without JWT returns 401`() {
    webTestClient.get()
      .uri("/cas1/premises/${premises.id}/beds")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting beds for a premises that does not exist returns 404`() {
    givenAUser { _, jwt ->
      webTestClient.get()
        .uri("/cas1/premises/${UUID.randomUUID()}/beds")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "approved-premises")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting beds for a premises returns a list of beds`() {
    givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { user, jwt ->

      val premises = givenAnApprovedPremises()

      val beds = listOf(
        givenAnApprovedPremisesBed(
          premises,
          characteristics = listOf(
            characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!,
            characteristicRepository.findCas1ByPropertyName("isWheelchairDesignated")!!,
          ),
        ),
        givenAnApprovedPremisesBed(
          premises,
          characteristics = listOf(
            characteristicRepository.findCas1ByPropertyName("isArsonSuitable")!!,
            characteristicRepository.findCas1ByPropertyName("hasStepFreeAccess")!!,
          ),
        ),
      )

      val otherPremises = givenAnApprovedPremises()

      bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { otherPremises }
          }
        }
      }

      val response = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBedSummary>()

      assertThat(response.size).isEqualTo(2)
      assertThat(response).containsExactlyInAnyOrder(
        Cas1PremisesBedSummary(
          id = beds[0].id,
          roomName = beds[0].room.name,
          bedName = beds[0].name,
          characteristics = listOf(Cas1SpaceCharacteristic.hasEnSuite, Cas1SpaceCharacteristic.isWheelchairDesignated),
        ),
        Cas1PremisesBedSummary(
          id = beds[1].id,
          roomName = beds[1].room.name,
          bedName = beds[1].name,
          characteristics = listOf(Cas1SpaceCharacteristic.isArsonSuitable, Cas1SpaceCharacteristic.hasStepFreeAccess),
        ),
      )
    }
  }
}
