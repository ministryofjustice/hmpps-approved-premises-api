package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.util.UUID

class Cas1BedSummaryTest : InitialiseDatabasePerClassTestBase() {
  lateinit var premises: PremisesEntity

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
  fun `Getting beds for a premises returns a list of beds, excluding ended beds`() {
    givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { user, jwt ->

      val premises = givenAnApprovedPremises()

      val bed1Active = givenAnApprovedPremisesBed(
        premises,
        characteristics = listOf(
          characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!,
          characteristicRepository.findCas1ByPropertyName("isWheelchairDesignated")!!,
        ),
      )

      val bed2Active = givenAnApprovedPremisesBed(
        premises,
        characteristics = listOf(
          characteristicRepository.findCas1ByPropertyName("isArsonSuitable")!!,
          characteristicRepository.findCas1ByPropertyName("hasStepFreeAccess")!!,
        ),
      )

      val bedNotQuiteEnded = givenAnApprovedPremisesBed(
        premises,
        characteristics = emptyList(),
        endDate = LocalDate.now().plusDays(2),
      )

      // ended, shouldn't appear
      givenAnApprovedPremisesBed(
        premises,
        characteristics = listOf(
          characteristicRepository.findCas1ByPropertyName("isArsonSuitable")!!,
        ),
        endDate = LocalDate.now(),
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

      assertThat(response.size).isEqualTo(3)
      assertThat(response).containsExactlyInAnyOrder(
        Cas1PremisesBedSummary(
          id = bed1Active.id,
          roomName = bed1Active.room.name,
          bedName = bed1Active.name,
          characteristics = listOf(Cas1SpaceCharacteristic.hasEnSuite, Cas1SpaceCharacteristic.isWheelchairDesignated),
        ),
        Cas1PremisesBedSummary(
          id = bed2Active.id,
          roomName = bed2Active.room.name,
          bedName = bed2Active.name,
          characteristics = listOf(Cas1SpaceCharacteristic.isArsonSuitable, Cas1SpaceCharacteristic.hasStepFreeAccess),
        ),
        Cas1PremisesBedSummary(
          id = bedNotQuiteEnded.id,
          roomName = bedNotQuiteEnded.room.name,
          bedName = bedNotQuiteEnded.name,
          characteristics = emptyList(),
        ),
      )
    }
  }
}
