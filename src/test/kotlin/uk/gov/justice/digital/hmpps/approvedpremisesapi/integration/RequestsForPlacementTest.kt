package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class RequestsForPlacementTest : IntegrationTestBase() {

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
    fun `Get all Requests for Placement for an application returns a 200 response with the expected value, newest first`() {
      givenAUser { user, jwt ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = null,
          createdByUser = user,
        ) { assessment, application ->
          val unsubmittedPlacementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withCreatedByUser(user)
          }

          val submittedInitialAutomaticPlacementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withCreatedByUser(user)
            withSubmittedAt(OffsetDateTime.parse("2007-12-03T10:15:30+01"))
            withExpectedArrival(LocalDate.now())
            withRequestedDuration(1)
            withPlacementType(PlacementType.AUTOMATIC)
            withAutomatic(true)
            withSentenceType(SentenceTypeOption.bailPlacement.name)
          }

          val submittedAdditionalPlacementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withCreatedByUser(user)
            withSubmittedAt(OffsetDateTime.parse("2007-11-03T10:15:30+01"))
            withExpectedArrival(LocalDate.now())
            withRequestedDuration(2)
            withReleaseType(ReleaseTypeOption.licence.name)
          }

          val submittedButReallocatedPlacementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withCreatedByUser(user)
            withSubmittedAt(OffsetDateTime.now())
            withReallocatedAt(OffsetDateTime.now())
            withExpectedArrival(LocalDate.now())
            withRequestedDuration(2)
          }

          val withdrawnPlacementApplication = placementApplicationFactory.produceAndPersist {
            withApplication(application)
            withCreatedByUser(user)
            withSubmittedAt(OffsetDateTime.parse("2007-10-03T10:15:30+01"))
            withIsWithdrawn(true)
            withWithdrawalReason(PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED)
            withExpectedArrival(LocalDate.now())
            withRequestedDuration(2)
          }

          val placementRequirements = placementRequirementsFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
            withEssentialCriteria(listOf())
            withDesirableCriteria(listOf())
          }

          val placementRequest = placementRequestFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.parse("2007-08-03T10:15:30+01"))
            withApplication(application)
            withAssessment(assessment)
            withPlacementRequirements(placementRequirements)
          }

          val withdrawnPlacementRequest = placementRequestFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.parse("2007-09-03T10:15:30+01"))
            withApplication(application)
            withAssessment(assessment)
            withPlacementRequirements(placementRequirements)
            withIsWithdrawn(true)
            withWithdrawalReason(PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST)
          }

          val requestForPlacements = webTestClient.get()
            .uri("/applications/${application.id}/requests-for-placement")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<RequestForPlacement>()

          assertThat(requestForPlacements).hasSize(5)
          assertThat(requestForPlacements[0].id).isEqualTo(submittedInitialAutomaticPlacementApplication.id)
          assertThat(requestForPlacements[0].type).isEqualTo(RequestForPlacementType.automatic)
          assertThat(requestForPlacements[0].status).isEqualTo(RequestForPlacementStatus.requestSubmitted)
          assertThat(requestForPlacements[0].sentenceType).isEqualTo(SentenceTypeOption.bailPlacement)
          assertThat(requestForPlacements[0].releaseType).isNull()
          assertThat(requestForPlacements[0].situation).isNull()

          assertThat(requestForPlacements[1].id).isEqualTo(submittedAdditionalPlacementApplication.id)
          assertThat(requestForPlacements[1].type).isEqualTo(RequestForPlacementType.manual)
          assertThat(requestForPlacements[1].status).isEqualTo(RequestForPlacementStatus.requestSubmitted)
          assertThat(requestForPlacements[1].sentenceType).isNull()
          assertThat(requestForPlacements[1].releaseType).isEqualTo(ReleaseTypeOption.licence)
          assertThat(requestForPlacements[1].situation).isNull()

          assertThat(requestForPlacements[2].id).isEqualTo(withdrawnPlacementApplication.id)
          assertThat(requestForPlacements[2].type).isEqualTo(RequestForPlacementType.manual)
          assertThat(requestForPlacements[2].status).isEqualTo(RequestForPlacementStatus.requestWithdrawn)
          assertThat(requestForPlacements[2].sentenceType).isNull()
          assertThat(requestForPlacements[0].releaseType).isNull()
          assertThat(requestForPlacements[2].situation).isNull()

          assertThat(requestForPlacements[3].id).isEqualTo(withdrawnPlacementRequest.id)
          assertThat(requestForPlacements[3].type).isEqualTo(RequestForPlacementType.automatic)
          assertThat(requestForPlacements[3].isWithdrawn).isTrue()
          assertThat(requestForPlacements[3].sentenceType).isNull()
          assertThat(requestForPlacements[0].releaseType).isNull()
          assertThat(requestForPlacements[3].situation).isNull()

          assertThat(requestForPlacements[4].id).isEqualTo(placementRequest.id)
          assertThat(requestForPlacements[4].type).isEqualTo(RequestForPlacementType.automatic)
          assertThat(requestForPlacements[4].status).isEqualTo(RequestForPlacementStatus.awaitingMatch)
          assertThat(requestForPlacements[4].sentenceType).isNull()
          assertThat(requestForPlacements[0].releaseType).isNull()
          assertThat(requestForPlacements[4].situation).isNull()
        }
      }
    }

    @Suppress("unused")
    @Test
    fun `Get legacy automatic Requests for Placement (no placement application)`() {
      givenAUser { user, jwt ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = null,
          createdByUser = user,
        ) { assessment, application ->
          val placementRequirements = placementRequirementsFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
            withDesirableCriteria(emptyList())
            withEssentialCriteria(emptyList())
          }

          val placementRequest = placementRequestFactory.produceAndPersist {
            withApplication(application)
            withAssessment(assessment)
            withPlacementRequirements(placementRequirements)
            withCreatedAt(OffsetDateTime.now())
            withPlacementApplication(null)
          }

          val requestForPlacements = webTestClient.get()
            .uri("/applications/${application.id}/requests-for-placement")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<RequestForPlacement>()

          assertThat(requestForPlacements).hasSize(1)
          assertThat(requestForPlacements[0].id).isEqualTo(placementRequest.id)
          assertThat(requestForPlacements[0].type).isEqualTo(RequestForPlacementType.automatic)
          assertThat(requestForPlacements[0].status).isEqualTo(RequestForPlacementStatus.awaitingMatch)
          assertThat(requestForPlacements[0].sentenceType).isNull()
          assertThat(requestForPlacements[0].situation).isNull()
        }
      }
    }
  }
}
