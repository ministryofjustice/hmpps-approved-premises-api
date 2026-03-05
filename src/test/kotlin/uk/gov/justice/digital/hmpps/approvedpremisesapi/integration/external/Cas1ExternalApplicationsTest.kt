package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.external

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.LocalDate

class Cas1ExternalApplicationsTest : IntegrationTestBase() {
  private val crn = "ABC1234"

  @Nested
  inner class GetSuitableApplicationsByCrn {
    @Test
    fun `Get suitable application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/external/cases/$crn/applications/suitable")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get suitable application without correct JWT authority returns 403`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/cas1/external/cases/$crn/applications/suitable")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get suitable application returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val premises = givenAnApprovedPremises()
          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
            withCrn(crn)
          }
          val placementApplication = givenAPlacementApplication(
            crn = crn,
            createdByUser = user,
            application = application,
          )

          val (placementRequest, applicationWithPr) = givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = crn,
            application = application,
            placementApplication = placementApplication,
          )

          givenACas1SpaceBooking(
            crn = crn,
            placementRequest = placementRequest,
            premises = premises,
            canonicalArrivalDate = LocalDate.of(2025, 5, 6),
            canonicalDepartureDate = LocalDate.of(2025, 5, 28),
            cancellationOccurredAt = null,
            application = applicationWithPr,
          )

          val suitableApplication = Cas1SuitableApplication(
            id = applicationWithPr.id,
            applicationStatus = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED,
            placementStatus = Cas1SpaceBookingStatus.UPCOMING,
            requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
            placementHistories = listOf(
              Cas1PlacementHistory(
                dateApplied = LocalDate.now(),
                placementStatus = Cas1SpaceBookingStatus.UPCOMING,
                requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              ),
            ),
          )

          val response = webTestClient.get()
            .uri("/cas1/external/cases/$crn/applications/suitable")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Cas1SuitableApplication::class.java)
            .returnResult()
            .responseBody

          Assertions.assertThat(response).isEqualTo(suitableApplication)
        }
      }
    }

    @Test
    fun `Get application with unsupported type returns bad request`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.get()
          .uri("/cas1/external/cases/$crn/applications/unsupported")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isBadRequest
      }
    }

    @Test
    fun `Get suitable application returns not found`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.get()
          .uri("/cas1/external/cases/$crn/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
