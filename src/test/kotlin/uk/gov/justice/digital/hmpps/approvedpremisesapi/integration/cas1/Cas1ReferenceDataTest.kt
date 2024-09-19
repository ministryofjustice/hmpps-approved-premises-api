package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.util.UUID

class Cas1ReferenceDataTest : IntegrationTestBase() {
  @Autowired
  lateinit var reasonTransformer: Cas1OutOfServiceBedReasonTransformer

  @Nested
  inner class GetOutOfServiceBedReasons {

    @Test
    fun success() {
      cas1OutOfServiceBedReasonTestRepository.deleteAll()

      val activeReason1 = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
        withIsActive(true)
        withName("Active reason 1")
      }

      val activeReason2 = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
        withIsActive(true)
        withName("Active reason 2")
      }

      cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
        withIsActive(false)
        withName("Inactive reason")
      }

      val expectedReasons = objectMapper.writeValueAsString(
        listOf(activeReason1, activeReason2).map { reason -> reasonTransformer.transformJpaToApi(reason) },
      )

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/cas1/reference-data/out-of-service-bed-reasons")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedReasons)
    }
  }

  @Nested
  inner class GetCruManagementAreas : InitialiseDatabasePerClassTestBase() {

    lateinit var area1: Cas1CruManagementAreaEntity
    lateinit var area2: Cas1CruManagementAreaEntity
    lateinit var womensEstateArea: Cas1CruManagementAreaEntity

    @BeforeAll
    fun setup() {
      cas1CruManagementAreaRepository.deleteAll()

      area1 = cas1CruManagementAreaEntityFactory.produceAndPersist {
        withId(UUID.randomUUID())
        withName("The area 1")
      }

      area2 = cas1CruManagementAreaEntityFactory.produceAndPersist {
        withId(UUID.randomUUID())
        withName("The area 1")
      }

      womensEstateArea = cas1CruManagementAreaEntityFactory.produceAndPersist {
        withId(Cas1CruManagementAreaEntity.WOMENS_ESTATE_ID)
        withName("The womens estate area")
      }
    }

    @Test
    fun `Success when women's estate enabled`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      mockFeatureFlagService.setFlag("cas1-womens-estate-enabled", true)

      val result = webTestClient.get()
        .uri("/cas1/reference-data/cru-management-areas")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1CruManagementArea>()

      assertThat(result).hasSize(3)
      assertThat(result[0].id).isEqualTo(area1.id)
      assertThat(result[0].name).isEqualTo(area1.name)

      assertThat(result[1].id).isEqualTo(area2.id)
      assertThat(result[1].name).isEqualTo(area2.name)

      assertThat(result[2].id).isEqualTo(womensEstateArea.id)
      assertThat(result[2].name).isEqualTo(womensEstateArea.name)
    }

    @Test
    fun `Success when women's estate disabled`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      mockFeatureFlagService.setFlag("cas1-womens-estate-enabled", false)

      val result = webTestClient.get()
        .uri("/cas1/reference-data/cru-management-areas")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1CruManagementArea>()

      assertThat(result).hasSize(2)
      assertThat(result[0].id).isEqualTo(area1.id)
      assertThat(result[0].name).isEqualTo(area1.name)

      assertThat(result[1].id).isEqualTo(area2.id)
      assertThat(result[1].name).isEqualTo(area2.name)
    }
  }
}
