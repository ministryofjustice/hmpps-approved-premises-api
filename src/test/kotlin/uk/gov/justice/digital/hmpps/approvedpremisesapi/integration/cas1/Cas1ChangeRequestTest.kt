package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1RejectChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_CREATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASubmittedCas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_CREATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_REJECTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_CHANGE_REQUEST_DEV
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRejectionReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.util.UUID

class Cas1ChangeRequestTest {

  @Nested
  inner class CreateChangeRequest : IntegrationTestBase() {

    @Test
    fun `Without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas1/placement-request/${UUID.randomUUID()}/change-request")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CHANGE_REQUEST_DEV", "CAS1_JANITOR"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Returns 403 when type is appeal and role is invalid`(role: UserRole) {
      givenAUser(roles = listOf(role)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/appeal")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.PLACEMENT_APPEAL,
                requestJson = "{}",
                reasonId = UUID.randomUUID(),
                spaceBookingId = UUID.randomUUID(),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CHANGE_REQUEST_DEV", "CAS1_JANITOR"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Returns 403 when type is planned transfer and role is invalid`(role: UserRole) {
      givenAUser(roles = listOf(role)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/planned-transfer")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.PLANNED_TRANSFER,
                requestJson = "{}",
                reasonId = UUID.randomUUID(),
                spaceBookingId = UUID.randomUUID(),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CHANGE_REQUEST_DEV", "CAS1_JANITOR"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Returns 400 when type is extension`(role: UserRole) {
      givenAUser(roles = listOf(role)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/extension")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.PLACEMENT_EXTENSION,
                requestJson = "{}",
                reasonId = UUID.randomUUID(),
                spaceBookingId = UUID.randomUUID(),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `Returns 200 when post new placement appeal change request is successful`() {
      givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          cruManagementArea = givenACas1CruManagementArea(emailAddress = "cruManagement@test.com"),
        ) { placementRequest, _ ->
          val spaceBooking = givenACas1SpaceBooking(
            crn = placementRequest.application.crn,
            placementRequest = placementRequest,
          )
          val changeRequestReason = cas1ChangeRequestReasonEntityFactory.produceAndPersist()
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/appeal")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.PLACEMENT_APPEAL,
                requestJson = "{}",
                reasonId = changeRequestReason.id,
                spaceBookingId = spaceBooking.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedChangeRequest = cas1ChangeRequestRepository.findAll()[0]
          assertThat(persistedChangeRequest.type).isEqualTo(ChangeRequestType.PLACEMENT_APPEAL)
          assertThat(persistedChangeRequest.requestJson).isEqualTo("{}")
          assertThat(persistedChangeRequest.requestReason).isEqualTo(changeRequestReason)
          assertThat(persistedChangeRequest.spaceBooking.id).isEqualTo(spaceBooking.id)

          domainEventAsserter.assertDomainEventOfTypeStored(placementRequest.application.id, APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_CREATED)
          emailAsserter.assertEmailRequested("cruManagement@test.com", Cas1NotifyTemplates.PLACEMENT_APPEAL_CREATED)
        }
      }
    }

    @Test
    fun `Returns 200 when post new planned transfer change request is successful`() {
      givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          cruManagementArea = givenACas1CruManagementArea(emailAddress = "cruManagement@test.com"),
        ) { placementRequest, _ ->
          val spaceBooking = givenACas1SpaceBooking(
            crn = placementRequest.application.crn,
            placementRequest = placementRequest,
            actualArrivalDate = LocalDate.now(),
          )
          val changeRequestReason = cas1ChangeRequestReasonEntityFactory.produceAndPersist {
            withChangeRequestType(ChangeRequestType.PLANNED_TRANSFER)
          }
          webTestClient.post()
            .uri("/cas1/placement-request/${placementRequest.id}/planned-transfer")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewChangeRequest(
                type = Cas1ChangeRequestType.PLANNED_TRANSFER,
                requestJson = "{}",
                reasonId = changeRequestReason.id,
                spaceBookingId = spaceBooking.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedChangeRequest = cas1ChangeRequestRepository.findAll()[0]
          assertThat(persistedChangeRequest.type).isEqualTo(ChangeRequestType.PLANNED_TRANSFER)
          assertThat(persistedChangeRequest.requestJson).isEqualTo("{}")
          assertThat(persistedChangeRequest.requestReason).isEqualTo(changeRequestReason)
          assertThat(persistedChangeRequest.spaceBooking.id).isEqualTo(spaceBooking.id)

          domainEventAsserter.assertDomainEventOfTypeStored(placementRequest.application.id, APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_CREATED)

          emailAsserter.assertEmailRequested("cruManagement@test.com", PLANNED_TRANSFER_REQUEST_CREATED)
        }
      }
    }
  }

  @Nested
  inner class FindOpen : InitialiseDatabasePerClassTestBase() {

    lateinit var cruManagementArea1: Cas1CruManagementAreaEntity
    lateinit var cruManagementArea1ChangeRequest1: Cas1ChangeRequestEntity

    lateinit var cruManagementArea2: Cas1CruManagementAreaEntity
    lateinit var cruManagementArea2ChangeRequest1: Cas1ChangeRequestEntity
    lateinit var cruManagementArea2ChangeRequest2: Cas1ChangeRequestEntity

    @BeforeAll
    fun setupChangeRequests() {
      val user = givenAUser().first

      cruManagementArea1 = givenACas1CruManagementArea()

      val placementRequest1 = givenAPlacementRequest(
        createdByUser = user,
        application = givenASubmittedCas1Application(
          createdByUser = user,
          offender = givenAnOffender(
            offenderDetailsConfigBlock = {
              withFirstName("Aaron")
              withLastName("Atkins")
            },
          ).first.asCaseSummary(),
          cruManagementArea = cruManagementArea1,
          tier = "B1",
        ),
      ).first

      cruManagementArea1ChangeRequest1 = givenACas1ChangeRequest(
        type = ChangeRequestType.PLACEMENT_APPEAL,
        spaceBooking = givenACas1SpaceBooking(
          crn = placementRequest1.application.crn,
          application = placementRequest1.application,
          placementRequest = placementRequest1,
          canonicalArrivalDate = LocalDate.of(2024, 6, 1),
          canonicalDepartureDate = LocalDate.of(2024, 6, 15),
        ),
        resolved = false,
        decisionJson = "{\"test\": 1}",
      )

      // is resolved, will be ignored
      givenACas1ChangeRequest(
        type = ChangeRequestType.PLACEMENT_APPEAL,
        decision = ChangeRequestDecision.APPROVED,
        spaceBooking = givenACas1SpaceBooking(
          crn = placementRequest1.application.crn,
          application = placementRequest1.application,
          placementRequest = placementRequest1,
        ),
        resolved = true,
        decisionJson = "{\"test\": 1}",
      )

      cruManagementArea2 = givenACas1CruManagementArea()

      val placementRequest2 = givenAPlacementRequest(
        createdByUser = user,
        application = givenASubmittedCas1Application(
          createdByUser = user,
          offender = givenAnOffender(
            offenderDetailsConfigBlock = {
              withFirstName("Berty")
              withLastName("Bats")
            },
          ).first.asCaseSummary(),
          cruManagementArea = cruManagementArea2,
          tier = "A1",
        ),
      ).first

      cruManagementArea2ChangeRequest1 = givenACas1ChangeRequest(
        type = ChangeRequestType.PLACEMENT_EXTENSION,
        spaceBooking = givenACas1SpaceBooking(
          crn = placementRequest2.application.crn,
          application = placementRequest2.application,
          placementRequest = placementRequest2,
          canonicalArrivalDate = LocalDate.of(2024, 3, 1),
          canonicalDepartureDate = LocalDate.of(2024, 3, 10),
        ),
        resolved = false,
        decisionJson = "{\"test\": 1}",
      )

      val placementRequest3 = givenAPlacementRequest(
        createdByUser = user,
        application = givenASubmittedCas1Application(
          createdByUser = user,
          offender = givenAnOffender(
            offenderDetailsConfigBlock = {
              withFirstName("Chris")
              withLastName("Chaplin")
            },
          ).first.asCaseSummary(),
          cruManagementArea = cruManagementArea2,
          tier = "C1",
        ),
      ).first

      cruManagementArea2ChangeRequest2 = givenACas1ChangeRequest(
        type = ChangeRequestType.PLANNED_TRANSFER,
        spaceBooking = givenACas1SpaceBooking(
          crn = placementRequest3.application.crn,
          application = placementRequest3.application,
          placementRequest = placementRequest3,
          canonicalArrivalDate = LocalDate.of(2024, 1, 1),
          canonicalDepartureDate = LocalDate.of(2024, 1, 5),
        ),
        resolved = false,
        decisionJson = "{\"test\": 1}",
      )
    }

    @Test
    fun `Getting change requests without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/placement-request/change-requests")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = ["CAS1_CHANGE_REQUEST_DEV", "CAS1_JANITOR"])
    fun `Getting change requests without a valid role returns 403`(role: UserRole) {
      val (_, jwt) = givenAUser(roles = listOf(role))

      webTestClient.get()
        .uri("/cas1/placement-request/change-requests")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `no results return empty`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

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
    fun `ensure fields correctly populated`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

      val response = webTestClient.get()
        .uri("/cas1/placement-request/change-requests")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>()
    }

    @Test
    fun `no filtering returns all open change requests`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

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
          cruManagementArea2ChangeRequest2.id,
        )
    }

    @Test
    fun `filter by cru management area`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

      val response = webTestClient.get()
        .uri("/cas1/placement-request/change-requests?cruManagementAreaId=${cruManagementArea1.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>()

      assertThat(response.map { it.id }).containsExactly(cruManagementArea1ChangeRequest1.id)
    }

    @Test
    fun `sort by name`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

      webTestClient.get()
        .uri("/cas1/placement-request/change-requests?sortBy=name&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>().let { response ->
          assertThat(response.map { it.id })
            .containsExactly(
              cruManagementArea1ChangeRequest1.id,
              cruManagementArea2ChangeRequest1.id,
              cruManagementArea2ChangeRequest2.id,
            )
        }

      webTestClient.get()
        .uri("/cas1/placement-request/change-requests?sortBy=name&sortDirection=desc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>().let { response ->
          assertThat(response.map { it.id })
            .containsExactly(
              cruManagementArea2ChangeRequest2.id,
              cruManagementArea2ChangeRequest1.id,
              cruManagementArea1ChangeRequest1.id,
            )
        }
    }

    @Test
    fun `sort by tier`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

      webTestClient.get()
        .uri("/cas1/placement-request/change-requests?sortBy=tier&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>().let { response ->
          assertThat(response.map { it.id })
            .containsExactly(
              cruManagementArea2ChangeRequest1.id,
              cruManagementArea1ChangeRequest1.id,
              cruManagementArea2ChangeRequest2.id,
            )
        }

      webTestClient.get()
        .uri("/cas1/placement-request/change-requests?sortBy=tier&sortDirection=desc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>().let { response ->
          assertThat(response.map { it.id })
            .containsExactly(
              cruManagementArea2ChangeRequest2.id,
              cruManagementArea1ChangeRequest1.id,
              cruManagementArea2ChangeRequest1.id,
            )
        }
    }

    @Test
    fun `sort by canonical arrival date`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

      webTestClient.get()
        .uri("/cas1/placement-request/change-requests?sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>().let { response ->
          assertThat(response.map { it.id })
            .containsExactly(
              cruManagementArea2ChangeRequest2.id,
              cruManagementArea2ChangeRequest1.id,
              cruManagementArea1ChangeRequest1.id,
            )
        }

      webTestClient.get()
        .uri("/cas1/placement-request/change-requests?sortBy=canonicalArrivalDate&sortDirection=desc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1ChangeRequestSummary>().let { response ->
          assertThat(response.map { it.id })
            .containsExactly(
              cruManagementArea1ChangeRequest1.id,
              cruManagementArea2ChangeRequest1.id,
              cruManagementArea2ChangeRequest2.id,
            )
        }
    }
  }

  @Nested
  inner class RejectChangeRequest : InitialiseDatabasePerClassTestBase() {

    lateinit var premises: ApprovedPremisesEntity

    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var cruManagementArea: Cas1CruManagementAreaEntity
    lateinit var placementAppealChangeRequest: Cas1ChangeRequestEntity

    lateinit var cruManagementArea2: Cas1CruManagementAreaEntity
    lateinit var alreadyRejectedChangeRequest: Cas1ChangeRequestEntity

    lateinit var placementRequest1: PlacementRequestEntity
    lateinit var placementRequest2: PlacementRequestEntity

    lateinit var placementAppealRejectionReason: Cas1ChangeRequestRejectionReasonEntity

    @BeforeAll
    fun setupChangeRequests() {
      val user = givenAUser().first

      cruManagementArea = givenACas1CruManagementArea(emailAddress = "cru@test.com")

      application = givenASubmittedCas1Application(
        createdByUser = user,
        offender = givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName("Allan")
            withLastName("Banks")
          },
        ).first.asCaseSummary(),
        cruManagementArea = cruManagementArea,
        tier = "A1",
      )

      placementRequest1 = givenAPlacementRequest(createdByUser = user, application = application).first

      premises = givenAnApprovedPremises(emailAddress = "premises@test.com")

      placementAppealChangeRequest = givenACas1ChangeRequest(
        type = ChangeRequestType.PLACEMENT_APPEAL,
        spaceBooking = givenACas1SpaceBooking(
          crn = application.crn,
          application = application,
          placementRequest = placementRequest1,
          canonicalArrivalDate = LocalDate.of(2024, 6, 1),
          canonicalDepartureDate = LocalDate.of(2024, 6, 15),
          premises = premises,
        ),
        decisionJson = "{\"test\": 1}",
      )

      cruManagementArea2 = givenACas1CruManagementArea()

      placementRequest2 = givenAPlacementRequest(
        createdByUser = user,
        application = givenASubmittedCas1Application(
          createdByUser = user,
          offender = givenAnOffender(
            offenderDetailsConfigBlock = {
              withFirstName("Roger")
              withLastName("Moor")
            },
          ).first.asCaseSummary(),
          cruManagementArea = cruManagementArea2,
          tier = "A2",
        ),
      ).first

      placementAppealRejectionReason = cas1ChangeRequestRejectionReasonEntityFactory
        .produceAndPersist {
          withChangeRequestType(ChangeRequestType.PLACEMENT_APPEAL)
        }
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = ["CAS1_CHANGE_REQUEST_DEV", "CAS1_JANITOR"])
    fun `Reject change request without a valid role returns 403`(role: UserRole) {
      val (_, jwt) = givenAUser(roles = listOf(role))

      webTestClient.patch()
        .uri("/cas1/placement-request/${placementRequest1.id}/change-requests/${placementAppealChangeRequest.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1RejectChangeRequest(
            rejectionReasonId = placementAppealRejectionReason.id,
            decisionJson = emptyMap(),
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `reject change request without JWT returns 401`() {
      webTestClient.patch()
        .uri("/cas1/placement-request/${placementRequest1.id}/change-requests/${placementAppealChangeRequest.id}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `return 200 when successfully reject placement appeal change request`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

      val changeRequestBefore = cas1ChangeRequestRepository.findById(placementAppealChangeRequest.id).get()
      assertThat(changeRequestBefore.rejectionReason).isNull()

      webTestClient.patch()
        .uri("/cas1/placement-request/${placementRequest1.id}/change-requests/${placementAppealChangeRequest.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1RejectChangeRequest(
            rejectionReasonId = placementAppealRejectionReason.id,
            decisionJson = emptyMap(),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val changeRequestAfter = cas1ChangeRequestRepository.findById(placementAppealChangeRequest.id).get()
      assertThat(changeRequestAfter.rejectionReason).isNotNull()
      assertThat(changeRequestAfter.rejectionReason?.changeRequestType).isEqualTo(ChangeRequestType.PLACEMENT_APPEAL)

      emailAsserter.assertEmailRequested("premises@test.com", Cas1NotifyTemplates.PLACEMENT_APPEAL_REJECTED)
      emailAsserter.assertEmailRequested("cru@test.com", Cas1NotifyTemplates.PLACEMENT_APPEAL_REJECTED)
      emailAsserter.assertEmailsRequestedCount(2)

      domainEventAsserter.assertDomainEventOfTypeStored(changeRequestBefore.placementRequest.application.id, APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_REJECTED)
    }

    @Test
    fun `return 200 when successfully reject planned transfer change request`() {
      val changeRequest = givenACas1ChangeRequest(
        type = ChangeRequestType.PLANNED_TRANSFER,
        spaceBooking = givenACas1SpaceBooking(
          crn = application.crn,
          application = application,
          placementRequest = placementRequest1,
          canonicalArrivalDate = LocalDate.of(2024, 6, 1),
          canonicalDepartureDate = LocalDate.of(2024, 6, 15),
          premises = premises,
        ),
        decisionJson = "{\"test\": 1}",
      )

      val reason = cas1ChangeRequestRejectionReasonEntityFactory
        .produceAndPersist {
          withChangeRequestType(ChangeRequestType.PLANNED_TRANSFER)
        }

      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

      webTestClient.patch()
        .uri("/cas1/placement-request/${placementRequest1.id}/change-requests/${changeRequest.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1RejectChangeRequest(
            rejectionReasonId = reason.id,
            decisionJson = emptyMap(),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val changeRequestAfter = cas1ChangeRequestRepository.findById(changeRequest.id).get()
      assertThat(changeRequestAfter.rejectionReason!!.id).isEqualTo(reason.id)

      emailAsserter.assertEmailsRequestedCount(1)
      emailAsserter.assertEmailRequested("premises@test.com", Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_REJECTED)

      domainEventAsserter.assertDomainEventOfTypeStored(application.id, APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_REJECTED)
    }
  }

  @Nested
  inner class GetChangeRequest : InitialiseDatabasePerClassTestBase() {

    lateinit var cruManagementArea: Cas1CruManagementAreaEntity
    lateinit var changeRequest: Cas1ChangeRequestEntity
    lateinit var placementRequest1: PlacementRequestEntity

    @BeforeAll
    fun setupChangeRequests() {
      val user = givenAUser().first

      cruManagementArea = givenACas1CruManagementArea()

      placementRequest1 = givenAPlacementRequest(
        createdByUser = user,
        application = givenASubmittedCas1Application(
          createdByUser = user,
          offender = givenAnOffender(
            offenderDetailsConfigBlock = {
              withFirstName("Allan")
              withLastName("Banks")
            },
          ).first.asCaseSummary(),
          cruManagementArea = cruManagementArea,
          tier = "A1",
        ),
      ).first

      changeRequest = givenACas1ChangeRequest(
        type = ChangeRequestType.PLACEMENT_APPEAL,
        decision = ChangeRequestDecision.APPROVED,
        decisionJson = "{\"test\": 1}",
        spaceBooking = givenACas1SpaceBooking(
          crn = placementRequest1.application.crn,
          application = placementRequest1.application,
          placementRequest = placementRequest1,
          canonicalArrivalDate = LocalDate.of(2024, 6, 1),
          canonicalDepartureDate = LocalDate.of(2024, 6, 15),
        ),

      )
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = ["CAS1_CHANGE_REQUEST_DEV", "CAS1_JANITOR"])
    fun `Get change request without a valid role returns 403`(role: UserRole) {
      val (_, jwt) = givenAUser(roles = listOf(role))

      webTestClient.get()
        .uri("/cas1/placement-request/${placementRequest1.id}/change-requests/${changeRequest.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `get change request without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/placement-request/${placementRequest1.id}/change-requests/${changeRequest.id}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `return 200 when successfully get change request`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CHANGE_REQUEST_DEV))

      val response = webTestClient.get()
        .uri("/cas1/placement-request/${placementRequest1.id}/change-requests/${changeRequest.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1ChangeRequest::class.java).responseBody.blockFirst()!!

      val changeRequestEntity = cas1ChangeRequestRepository.findById(changeRequest.id).get()
      assertThat(response.id).isEqualTo(changeRequestEntity.id)
      assertThat(response.type).isEqualTo(Cas1ChangeRequestType.valueOf(changeRequestEntity.type.name))
      assertThat(response.requestReason).isEqualTo(NamedId(changeRequestEntity.requestReason.id, changeRequestEntity.requestReason.code))
      assertThat(response.rejectionReason).isNull()
      assertThat(response.createdAt).isEqualTo(changeRequestEntity.createdAt.toInstant())
      assertThat(response.updatedAt).isEqualTo(changeRequestEntity.updatedAt.toInstant())
      assertThat(response.decision).isEqualTo(Cas1ChangeRequestDecision.APPROVED)
      assertThat(objectMapper.writeValueAsString(response.decisionJson)).isEqualTo("{\"test\":1}")
      assertThat(objectMapper.writeValueAsString(response.requestJson)).isEqualTo("\"{}\"")
      assertThat(response.spaceBookingId).isEqualTo(changeRequestEntity.spaceBooking.id)
    }
  }
}
