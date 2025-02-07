package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1BedSummaryTransformer
import java.util.UUID

class Cas1BedSummaryTest : InitialiseDatabasePerClassTestBase() {
  lateinit var premises: PremisesEntity
  lateinit var probationRegion: ProbationRegionEntity
  lateinit var localAuthorityArea: LocalAuthorityAreaEntity

  @Autowired
  lateinit var cas1BedSummaryTransformer: Cas1BedSummaryTransformer

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

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      val otherPremises = approvedPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityArea)
      }

      bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { otherPremises }
          }
        }
      }

      val expectedJson = objectMapper.writeValueAsString(
        listOf(
          cas1BedSummaryTransformer.transformEntityToApi(bed),
        ),
      )

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }
}
