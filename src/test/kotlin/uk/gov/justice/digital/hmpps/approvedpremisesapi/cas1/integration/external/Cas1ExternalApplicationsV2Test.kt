package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.SAS_ROLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as AssessmentDecisionJpa

class Cas1ExternalApplicationsV2Test : IntegrationTestBase() {
  private val crn = "ABC1234"

  @Nested
  inner class GetCurrentApplication {

    @Test
    fun `Get current application v2 without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/external/cases/$crn/applications/current")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get current application v2 without correct JWT authority returns 403`() {
      val (_, jwt) = givenAUser()

      webTestClient.get()
        .uri("/cas1/external/cases/$crn/applications/current")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Get current application v2 when no application exists returns 204`() {
      webTestClient.get()
        .uri("/cas1/external/cases/$crn/applications/current")
        .header("Authorization", "Bearer $clientCredentialsJwt")
        .exchange()
        .expectStatus()
        .isNoContent
    }

    @Test
    fun `Get current application v2 returns application and assessment when there are no requests for placement`() {
      val (user, _) = givenAUser()
      val (_, application) = givenAnAssessmentForApprovedPremises(
        allocatedToUser = null,
        createdByUser = user,
        decision = AssessmentDecisionJpa.REJECTED,
      )
      application.status = ApprovedPremisesApplicationStatus.REJECTED
      approvedPremisesApplicationRepository.save(application)

      val response = getSuitableApplication(application.crn)

      assertThat(response.id).isEqualTo(application.id)
      assertThat(response.uiUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(response.status).isEqualTo(ApprovedPremisesApplicationStatus.REJECTED)
      assertThat(response.createdBy.username).isEqualTo(user.deliusUsername)
      val submitted = response.submittedApplication!!
      assertThat(submitted.assessment!!.decision).isEqualTo(AssessmentDecision.rejected)
      assertThat(submitted.upcomingRequestForPlacement).isNull()
      assertThat(submitted.requestForPlacementHistory).isEmpty()
    }

    @Test
    fun `Get current application v2 returns upcoming request for placement and history when there are several requests for placement, one with several bookings`() {
      val (user, _) = givenAUser()
      val (_, application) = givenAnAssessmentForApprovedPremises(
        allocatedToUser = null,
        createdByUser = user,
        decision = AssessmentDecisionJpa.ACCEPTED,
      )
      application.status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED
      approvedPremisesApplicationRepository.save(application)

      val premises = givenAnApprovedPremises(supportsSpaceBookings = true)
      val today = LocalDate.now()

      // past
      givenAPlacementRequest(
        createdByUser = user,
        application = application,
        expectedArrival = today.minusDays(100),
        duration = 7,
      )

      // past, arrived
      val placementApplicationWithBookings = givenAPlacementApplication(
        createdByUser = user,
        application = application,
        submittedAt = OffsetDateTime.now(),
        decision = PlacementApplicationDecision.ACCEPTED,
        expectedArrival = today.minusDays(70),
        requestedDuration = 14,
      )
      val (requestWithBookings, _) = givenAPlacementRequest(
        createdByUser = user,
        application = application,
        placementApplication = placementApplicationWithBookings,
        expectedArrival = today.minusDays(65),
        duration = 14,
      )
      val pastBooking = givenACas1SpaceBooking(
        crn = application.crn,
        premises = premises,
        application = application,
        placementRequest = requestWithBookings,
        expectedArrivalDate = today.minusDays(60),
        expectedDepartureDate = today.minusDays(46),
        actualArrivalDate = today.minusDays(59),
      )

      // future
      givenAPlacementApplication(
        createdByUser = user,
        application = application,
        submittedAt = OffsetDateTime.now(),
        expectedArrival = today.plusDays(90),
        requestedDuration = 10,
      )

      // upcoming
      val futureBooking = givenACas1SpaceBooking(
        crn = application.crn,
        premises = premises,
        application = application,
        placementRequest = requestWithBookings,
        expectedArrivalDate = today.plusDays(30),
        expectedDepartureDate = today.plusDays(44),
      )

      val response = getSuitableApplication(application.crn)

      val submitted = response.submittedApplication!!
      val upcoming = submitted.upcomingRequestForPlacement!!
      assertThat(upcoming.placement!!.status).isEqualTo(Cas1SpaceBookingStatus.UPCOMING)
      assertThat(upcoming.placement!!.expectedArrivalDate).isEqualTo(futureBooking.expectedArrivalDate)
      assertThat(upcoming.requestForPlacement.expectedArrivalDate).isEqualTo(today.minusDays(70))
      assertThat(upcoming.requestForPlacement.durationDays).isEqualTo(14)

      assertThat(submitted.requestForPlacementHistory).hasSize(2)
      assertThat(submitted.requestForPlacementHistory.filter { it.placement == null }).hasSize(1)

      val arrived = submitted.requestForPlacementHistory.single { it.placement != null }
      assertThat(arrived.placement!!.expectedArrivalDate).isEqualTo(pastBooking.expectedArrivalDate)
      assertThat(arrived.placement!!.actualArrivalDate).isEqualTo(pastBooking.actualArrivalDate)
      assertThat(arrived.placement!!.premises.addressLine1).isEqualTo(premises.addressLine1)
      assertThat(arrived.requestForPlacement.expectedArrivalDate).isEqualTo(today.minusDays(70))
    }

    private val clientCredentialsJwt: String
      get() = jwtAuthHelper.createValidClientCredentialsJwt(role = SAS_ROLE)

    private fun getSuitableApplication(crn: String) = webTestClient.get()
      .uri("/cas1/external/cases/$crn/applications/current")
      .header("Authorization", "Bearer $clientCredentialsJwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(Cas1ExternalApplicationV2Dto::class.java)
      .returnResult()
      .responseBody!!
  }
}
