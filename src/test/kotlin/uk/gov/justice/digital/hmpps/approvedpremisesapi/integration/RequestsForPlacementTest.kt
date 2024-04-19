package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Approved Premises`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime
import java.util.UUID

class RequestsForPlacementTest : IntegrationTestBase() {
  @Autowired
  lateinit var requestForPlacementTransformer: RequestForPlacementTransformer

  @Nested
  inner class AllRequestsForPlacementForApplication {
    @Test
    fun `Get all Requests for Placement for an application without a JWT returns a 401 response`() {
      webTestClient.get()
        .uri("/applications/${UUID.randomUUID()}/requests-for-placement")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all Requests for Placement for an application that could not be found returns a 404 response`() {
      `Given a User` { _, jwt ->
        webTestClient.get()
          .uri("/applications/${UUID.randomUUID()}/requests-for-placement")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Get all Requests for Placement for an application returns a 200 response with the expected value`() {
      `Given a User` { user, jwt ->
        `Given an Assessment for Approved Premises`(
          allocatedToUser = null,
          createdByUser = user,
        ) { assessment, application ->
          val schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist()

          val placementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withSchemaVersion(schema)
            withCreatedByUser(user)
          }

          val postCodeDistrict = postCodeDistrictFactory.produceAndPersist()

          val placementRequirements = placementRequirementsFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPostcodeDistrict(postCodeDistrict)
            withEssentialCriteria(listOf())
            withDesirableCriteria(listOf())
          }

          val placementRequest = placementRequestFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPlacementRequirements(placementRequirements)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          }

          webTestClient.get()
            .uri("/applications/${application.id}/requests-for-placement")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication),
                  requestForPlacementTransformer.transformPlacementRequestEntityToApi(placementRequest),
                ),
              ),
            )
        }
      }
    }
  }

  @Nested
  inner class SpecificRequestForPlacement {
    @Test
    fun `Get a Request for Placement for an application without a JWT returns a 401 response`() {
      webTestClient.get()
        .uri("/applications/${UUID.randomUUID()}/requests-for-placement/${UUID.randomUUID()}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get a Request for Placement for an application that could not be found returns a 404 response`() {
      `Given a User` { _, jwt ->
        webTestClient.get()
          .uri("/applications/${UUID.randomUUID()}/requests-for-placement/${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Get a Request for Placement that does not exist returns a 404 response`() {
      `Given a User` { user, jwt ->
        `Given an Application`(user) { wrongApplication ->
          webTestClient.get()
            .uri("/applications/${wrongApplication.id}/requests-for-placement/${UUID.randomUUID()}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isNotFound
        }
      }
    }

    @Test
    fun `Get a Request for Placement for an application that is not the application it belongs to returns a 404 response`() {
      `Given a User` { user, jwt ->
        `Given an Application`(user) { wrongApplication ->
          `Given a Placement Request`(
            placementRequestAllocatedTo = null,
            assessmentAllocatedTo = user,
            createdByUser = user,
          ) { placementRequest, _ ->
            webTestClient.get()
              .uri("/applications/${wrongApplication.id}/requests-for-placement/${placementRequest.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }
    }

    @Test
    fun `Get a Request for Placement for an application returns a 200 response with the expected placement request`() {
      `Given a User` { user, jwt ->
        `Given an Assessment for Approved Premises`(
          allocatedToUser = null,
          createdByUser = user,
        ) { assessment, application ->
          val postCodeDistrict = postCodeDistrictFactory.produceAndPersist()

          val placementRequirements = placementRequirementsFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPostcodeDistrict(postCodeDistrict)
            withEssentialCriteria(listOf())
            withDesirableCriteria(listOf())
          }

          val placementRequest = placementRequestFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPlacementRequirements(placementRequirements)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          }

          webTestClient.get()
            .uri("/applications/${application.id}/requests-for-placement/${placementRequest.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                requestForPlacementTransformer.transformPlacementRequestEntityToApi(placementRequest),
              ),
            )
        }
      }
    }

    @Test
    fun `Get a Request for Placement for an application returns a 200 response with the expected placement application`() {
      `Given a User` { user, jwt ->
        val schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist()

        `Given a Placement Application`(createdByUser = user, schema = schema) { placementApplication ->
          webTestClient.get()
            .uri("/applications/${placementApplication.application.id}/requests-for-placement/${placementApplication.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication),
              ),
            )
        }
      }
    }
  }
}
