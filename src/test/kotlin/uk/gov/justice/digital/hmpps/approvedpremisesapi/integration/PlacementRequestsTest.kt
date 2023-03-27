package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import java.time.OffsetDateTime

class PlacementRequestsTest : IntegrationTestBase() {

  @Autowired
  lateinit var placementRequestTransformer: PlacementRequestTransformer

  @Test
  fun `Get all placement requests without JWT returns 401`() {
    webTestClient.get()
      .uri("/placement-requests")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `It returns all the placement requests for a user`() {
    `Given a User` { user, jwt ->
      `Given an Offender` { offenderDetails1, inmateDetails1 ->
        `Given an Offender` { offenderDetails2, _ ->

          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val application1 = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails1.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
          }

          val application2 = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails2.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
          }

          val placementRequest = placementRequestFactory.produceAndPersist {
            withAllocatedToUser(user)
            withApplication(application1)
            withPostcodeDistrict(
              postCodeDistrictRepository.findAll()[0]
            )
            withDesirableCriteria(
              characteristicEntityFactory.produceAndPersistMultiple(5)
            )
            withEssentialCriteria(
              characteristicEntityFactory.produceAndPersistMultiple(3)
            )
          }

          placementRequestFactory.produceAndPersistMultiple(2) {
            withAllocatedToUser(user)
            withReallocatedAt(OffsetDateTime.now())
            withApplication(application2)
            withPostcodeDistrict(
              postCodeDistrictRepository.findAll()[0]
            )
            withDesirableCriteria(
              characteristicEntityFactory.produceAndPersistMultiple(5)
            )
            withEssentialCriteria(
              characteristicEntityFactory.produceAndPersistMultiple(3)
            )
          }

          webTestClient.get()
            .uri("/placement-requests")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(placementRequest, offenderDetails1, inmateDetails1)
                )
              )
            )
        }
      }
    }
  }
}
