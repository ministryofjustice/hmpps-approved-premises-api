package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.LocalDate
import java.time.OffsetDateTime

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
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = null,
            createdByUser = user,
          ) { assessment, application ->

            application.status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED

            approvedPremisesApplicationRepository.save(application)

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

            val region = givenAProbationRegion()

            val premises = givenAnApprovedPremises(
              region = region,
              supportsSpaceBookings = true,
            )

            val (offender) = givenAnOffender()

            cas1SpaceBookingEntityFactory.produceAndPersist {
              withCrn(offender.otherIds.crn)
              withPremises(premises)
              withPlacementRequest(placementRequest)
              withApplication(placementRequest.application)
              withCreatedBy(user)
              withExpectedArrivalDate(LocalDate.now())
              withExpectedDepartureDate(LocalDate.now().plusDays(10))
            }

            val suitableApplication = Cas1SuitableApplication(
              id = application.id,
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
              .uri("/cas1/external/cases/${application.crn}/applications/suitable")
              .header("Authorization", "Bearer $clientCredentialsJwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody(Cas1SuitableApplication::class.java)
              .returnResult()
              .responseBody

            assertThat(response).isEqualTo(suitableApplication)
          }
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
