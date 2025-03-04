package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1BedDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.util.UUID

class Cas1BedDetailTest : InitialiseDatabasePerClassTestBase() {
  lateinit var premises: PremisesEntity
  lateinit var probationRegion: ProbationRegionEntity
  lateinit var localAuthorityArea: LocalAuthorityAreaEntity

  @BeforeEach
  fun setup() {
    probationRegion = givenAProbationRegion()
    localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    this.premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
    }
  }

  @Test
  fun `Getting a bed for a premises without JWT returns 401`() {
    webTestClient.get()
      .uri("/cas1/premises/${premises.id}/beds/${UUID.randomUUID()}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting a bed for a premises that does not exist returns 404`() {
    givenAUser { _, jwt ->
      webTestClient.get()
        .uri("/cas1/premises/${UUID.randomUUID()}/beds/${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "approved-premises")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting a bed for a premises returns the bed`() {
    givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { user, jwt ->

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
            withCharacteristics(*(listOf("hasEnSuite", "isGroundFloor").map { characteristicRepository.findByPropertyNameAndScopes(it, ServiceName.approvedPremises.value, "room")!! }).toTypedArray())
          }
        }
      }

      val response = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/beds/${bed.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1BedDetail>()

      Assertions.assertThat(response.id).isEqualTo(bed.id)
      Assertions.assertThat(response.characteristics).containsExactlyInAnyOrder(Cas1SpaceCharacteristic.hasEnSuite, Cas1SpaceCharacteristic.isGroundFloor)
    }
  }
}
