package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer

class Cas1ReferenceDataTest : IntegrationTestBase() {
  @Autowired
  lateinit var reasonTransformer: Cas1OutOfServiceBedReasonTransformer

  @Test
  fun `All available out-of-service bed reasons are returned`() {
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
