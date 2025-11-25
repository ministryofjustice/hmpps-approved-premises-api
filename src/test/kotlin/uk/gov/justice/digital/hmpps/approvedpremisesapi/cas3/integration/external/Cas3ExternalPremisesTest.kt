package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import java.time.LocalDate

class Cas3ExternalPremisesTest : Cas3IntegrationTestBase() {

  @Nested
  inner class GetPremisesById {

    @Test
    fun `Get Premises by ID when no JWT returns 401 Unauthorized`() {
      givenAUser { user, jwt ->
        val premises = create5Cas3PremisesAndDependencyData(user.probationRegion)
        val premisesToGet = premises.drop(1).first()
        webTestClient.get()
          .uri("/cas3/external/premises/${premisesToGet.id}")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    @Test
    fun `Get Premises by ID when user JWT returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val premises = create5Cas3PremisesAndDependencyData(user.probationRegion)
        val premisesToGet = premises.drop(1).first()
        webTestClient.get()
          .uri("/cas3/external/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get Premises by ID returns OK when SAS client_credentials JWT`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        val probationRegion = probationRegionEntityFactory.produceAndPersist()
        val premises = create5Cas3PremisesAndDependencyData(probationRegion)
        val premisesToGet = premises.drop(1).first()
        val responseBody = webTestClient.get()
          .uri("/cas3/external/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        assertThat(responseBody).isEqualTo(
          objectMapper.writeValueAsString(
            createCas3Premises(
              premisesToGet,
              probationRegion,
              premisesToGet.probationDeliveryUnit!!,
              premisesToGet.localAuthorityArea!!,
              Cas3PremisesStatus.online,
              totalOnlineBedspaces = 2,
              totalUpcomingBedspaces = 1,
              totalArchivedBedspaces = 1,
            ),
          ),
        )
      }
    }
  }

  private fun create5Cas3PremisesAndDependencyData(probationRegion: ProbationRegionEntity): List<TemporaryAccommodationPremisesEntity> {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }
    return getListPremisesByStatus(
      probationRegion = probationRegion,
      probationDeliveryUnit = probationDeliveryUnit,
      localAuthorityArea = localAuthorityArea,
      numberOfPremises = 5,
      propertyStatus = PropertyStatus.active,
    ).map { premises ->
      // online bedspaces
      createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateBefore(360), endDate = null)
      createBedspaceInPremises(
        premises,
        startDate = LocalDate.now().randomDateBefore(360),
        endDate = LocalDate.now().plusDays(1).randomDateAfter(90),
      )

      // upcoming bedspaces
      createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateAfter(30), endDate = null)

      // archived bedspaces
      createBedspaceInPremises(
        premises,
        startDate = LocalDate.now().minusDays(180).randomDateBefore(120),
        endDate = LocalDate.now().minusDays(1).randomDateBefore(90),
      )

      premises
    }
  }
}
