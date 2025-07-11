package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
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
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/applications/${UUID.randomUUID()}/requests-for-placement")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Suppress("unused")
    @Test
    fun `Get all Requests for Placement for an application returns a 200 response with the expected value`() {
      givenAUser { user, jwt ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = null,
          createdByUser = user,
        ) { assessment, application ->
          val unsubmittedPlacementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withCreatedByUser(user)
          }

          val submittedPlacementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withCreatedByUser(user)
            withSubmittedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withExpectedArrival(LocalDate.now())
            withDuration(1)
          }

          val submittedButReallocatedPlacementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withCreatedByUser(user)
            withSubmittedAt(OffsetDateTime.now())
            withReallocatedAt(OffsetDateTime.now())
            withExpectedArrival(LocalDate.now())
            withDuration(1)
          }

          val withdrawnPlacementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withCreatedByUser(user)
            withSubmittedAt(OffsetDateTime.now())
            withIsWithdrawn(true)
            withWithdrawalReason(PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED)
            withExpectedArrival(LocalDate.now())
            withDuration(1)
          }

          val placementRequirements = placementRequirementsFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
            withEssentialCriteria(listOf())
            withDesirableCriteria(listOf())
          }

          val placementRequest = placementRequestFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPlacementRequirements(placementRequirements)
            withCreatedAt(OffsetDateTime.now())
          }

          val withdrawnPlacementRequest = placementRequestFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPlacementRequirements(placementRequirements)
            withCreatedAt(OffsetDateTime.now())
            withIsWithdrawn(true)
            withWithdrawalReason(PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST)
          }

          val reallocatedPlacementRequest = placementRequestFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPlacementRequirements(placementRequirements)
            withCreatedAt(OffsetDateTime.now())
            withReallocatedAt(OffsetDateTime.now())
          }

          val requestForPlacements = webTestClient.get()
            .uri("/applications/${application.id}/requests-for-placement")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<RequestForPlacement>()

          assertThat(requestForPlacements).hasSize(4)
          assertThat(requestForPlacements[0].id).isEqualTo(submittedPlacementApplication.id)
          assertThat(requestForPlacements[0].type).isEqualTo(RequestForPlacementType.manual)
          assertThat(requestForPlacements[0].status).isEqualTo(RequestForPlacementStatus.requestSubmitted)

          assertThat(requestForPlacements[1].id).isEqualTo(withdrawnPlacementApplication.id)
          assertThat(requestForPlacements[1].type).isEqualTo(RequestForPlacementType.manual)
          assertThat(requestForPlacements[1].status).isEqualTo(RequestForPlacementStatus.requestWithdrawn)

          assertThat(requestForPlacements[2].id).isEqualTo(placementRequest.id)
          assertThat(requestForPlacements[2].type).isEqualTo(RequestForPlacementType.automatic)
          assertThat(requestForPlacements[2].status).isEqualTo(RequestForPlacementStatus.awaitingMatch)

          assertThat(requestForPlacements[3].id).isEqualTo(withdrawnPlacementRequest.id)
          assertThat(requestForPlacements[3].type).isEqualTo(RequestForPlacementType.automatic)
          assertThat(requestForPlacements[3].isWithdrawn).isTrue()
        }
      }
    }
  }
}
