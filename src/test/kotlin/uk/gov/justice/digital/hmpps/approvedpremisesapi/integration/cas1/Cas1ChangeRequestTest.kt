package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_FUTURE_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects

class Cas1ChangeRequestTest : IntegrationTestBase() {

  @Nested
  inner class FindOpen : InitialiseDatabasePerClassTestBase() {

    // TODO: no jwt
    // TODO: wrong role
    // TODO: exclude requests with decisions

    lateinit var cruManagementArea1: Cas1CruManagementAreaEntity
    lateinit var cruManagementArea1ChangeRequest1: Cas1ChangeRequestEntity

    lateinit var cruManagementArea2: Cas1CruManagementAreaEntity
    lateinit var cruManagementArea2ChangeRequest1: Cas1ChangeRequestEntity

    @BeforeAll
    fun setupChangeRequests() {
      val user = givenAUser().first

      cruManagementArea1 = givenACas1CruManagementArea()

      val placementRequest1 = givenAPlacementRequest(
        createdByUser = user,
        application = givenACas1Application(
          createdByUser = user,
          crn = givenAnOffender().first.otherIds.crn,
          cruManagementArea = cruManagementArea1,
        ),
      ).first

      cruManagementArea1ChangeRequest1 = givenACas1ChangeRequest(
        type = ChangeRequestType.APPEAL,
        decision = null,
        spaceBooking = givenACas1SpaceBooking(
          crn = placementRequest1.application.crn,
          application = placementRequest1.application,
          placementRequest = placementRequest1,
        ),
      )

      givenACas1ChangeRequest(
        type = ChangeRequestType.APPEAL,
        decision = ChangeRequestDecision.APPROVED,
        spaceBooking = givenACas1SpaceBooking(
          crn = placementRequest1.application.crn,
          application = placementRequest1.application,
          placementRequest = placementRequest1,
        ),
      )

      cruManagementArea2 = givenACas1CruManagementArea()

      val placementRequest2 = givenAPlacementRequest(
        createdByUser = user,
        application = givenACas1Application(
          createdByUser = user,
          crn = givenAnOffender().first.otherIds.crn,
          cruManagementArea = cruManagementArea2,
        ),
      ).first

      cruManagementArea2ChangeRequest1 = givenACas1ChangeRequest(
        type = ChangeRequestType.APPEAL,
        decision = null,
        spaceBooking = givenACas1SpaceBooking(
          crn = placementRequest2.application.crn,
          application = placementRequest2.application,
          placementRequest = placementRequest2,
        ),
      )
    }

    @Test
    fun `no results return empty`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val emptyCruManagementArea = givenACas1CruManagementArea()

      val response = webTestClient.get()
        .uri("/cas1/placement-request/change-requests?cruManagementAreaId=${emptyCruManagementArea.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>()

      assertThat(response).isEmpty()
    }

    @Test
    fun `no filtering returns all open change requests`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/placement-request/change-requests")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>()

      assertThat(response.map { it.id })
        .containsExactlyInAnyOrder(
          cruManagementArea1ChangeRequest1.id,
          cruManagementArea2ChangeRequest1.id,
        )
    }

    @Test
    fun `filter by cru management area`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/placement-request/change-requests?cruManagementAreaId=${cruManagementArea1.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>()

      assertThat(response.map { it.id }).containsExactly(cruManagementArea1ChangeRequest1.id)
    }

    // TODO: all sorting options
  }
}
