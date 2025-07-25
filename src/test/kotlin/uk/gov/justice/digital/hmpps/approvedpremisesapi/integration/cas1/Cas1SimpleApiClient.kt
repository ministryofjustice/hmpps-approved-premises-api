package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Component
class Cas1SimpleApiClient {

  fun applicationSubmit(
    integrationTestBase: IntegrationTestBase,
    id: UUID,
    applicantJwt: String,
    body: SubmitApprovedPremisesApplication,
  ) {
    integrationTestBase.webTestClient.post()
      .uri("/applications/$id/submission")
      .header("Authorization", "Bearer $applicantJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun applicationWithdraw(
    integrationTestBase: IntegrationTestBase,
    id: UUID,
    body: NewWithdrawal,
  ) {
    val managerJwt = integrationTestBase.givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)).second

    integrationTestBase.webTestClient.post()
      .uri("/applications/$id/withdrawal")
      .header("Authorization", "Bearer $managerJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun assessmentAppealCreate(
    integrationTestBase: IntegrationTestBase,
    applicationId: UUID,
    body: NewAppeal,
  ) {
    val appealsManagerJwt = integrationTestBase.givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)).second

    integrationTestBase.webTestClient.post()
      .uri("/applications/$applicationId/appeals")
      .header("Authorization", "Bearer $appealsManagerJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isCreated
  }

  fun assessmentUpdate(
    integrationTestBase: IntegrationTestBase,
    assessmentId: UUID,
    assessorJwt: String,
    body: UpdateAssessment,
  ) {
    integrationTestBase.webTestClient.put()
      .uri("/cas1/assessments/$assessmentId")
      .header("Authorization", "Bearer $assessorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun assessmentAccept(
    integrationTestBase: IntegrationTestBase,
    assessmentId: UUID,
    assessorJwt: String,
    body: AssessmentAcceptance,
  ) {
    integrationTestBase.webTestClient.post()
      .uri("/cas1/assessments/$assessmentId/acceptance")
      .header("Authorization", "Bearer $assessorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun assessmentReject(
    integrationTestBase: IntegrationTestBase,
    assessmentId: UUID,
    assessorJwt: String,
    body: AssessmentRejection,
  ) {
    integrationTestBase.webTestClient.post()
      .uri("/cas1/assessments/$assessmentId/rejection")
      .header("Authorization", "Bearer $assessorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun assessmentReallocate(
    integrationTestBase: IntegrationTestBase,
    assessmentId: UUID,
    targetUserId: UUID,
  ) {
    val managerJwt = integrationTestBase.givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)).second

    integrationTestBase.webTestClient.post()
      .uri("/tasks/assessment/$assessmentId/allocations")
      .header("Authorization", "Bearer $managerJwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .bodyValue(
        NewReallocation(
          userId = targetUserId,
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated
  }

  fun assessmentCreateClarificationNote(
    integrationTestBase: IntegrationTestBase,
    assessmentId: UUID,
    assessorJwt: String,
    body: NewClarificationNote,
  ): ClarificationNote = integrationTestBase.webTestClient.post()
    .uri("/cas1/assessments/$assessmentId/notes")
    .header("Authorization", "Bearer $assessorJwt")
    .bodyValue(body)
    .exchange()
    .expectStatus()
    .isOk
    .returnResult<ClarificationNote>()
    .responseBody
    .blockFirst()!!

  fun assessmentUpdateClarificationNote(
    integrationTestBase: IntegrationTestBase,
    assessmentId: UUID,
    noteId: UUID,
    assessorJwt: String,
    body: UpdatedClarificationNote,
  ) {
    integrationTestBase.webTestClient.put()
      .uri("/cas1/assessments/$assessmentId/notes/$noteId")
      .header("Authorization", "Bearer $assessorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun placementApplicationCreate(
    integrationTestBase: IntegrationTestBase,
    creatorJwt: String,
    body: NewPlacementApplication,
  ) {
    integrationTestBase.webTestClient.post()
      .uri("/cas1/placement-applications")
      .header("Authorization", "Bearer $creatorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun placementApplicationDecision(
    integrationTestBase: IntegrationTestBase,
    placementApplicationId: UUID,
    assessorJwt: String,
    body: PlacementApplicationDecisionEnvelope,
  ) {
    integrationTestBase.webTestClient.post()
      .uri("/cas1/placement-applications/$placementApplicationId/decision")
      .header("Authorization", "Bearer $assessorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun placementApplicationUpdate(
    integrationTestBase: IntegrationTestBase,
    placementApplicationId: UUID,
    creatorJwt: String,
    body: UpdatePlacementApplication,
  ) {
    integrationTestBase.webTestClient.put()
      .uri("/cas1/placement-applications/$placementApplicationId")
      .header("Authorization", "Bearer $creatorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun placementApplicationWithdraw(
    integrationTestBase: IntegrationTestBase,
    placementApplicationId: UUID,
    body: WithdrawPlacementApplication,
  ) {
    val managerJwt = integrationTestBase.givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)).second

    integrationTestBase.webTestClient.post()
      .uri("/cas1/placement-applications/$placementApplicationId/withdraw")
      .header("Authorization", "Bearer $managerJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun placementApplicationSubmit(
    integrationTestBase: IntegrationTestBase,
    placementApplicationId: UUID,
    creatorJwt: String,
    body: SubmitPlacementApplication,
  ) {
    integrationTestBase.webTestClient.post()
      .uri("/cas1/placement-applications/$placementApplicationId/submission")
      .header("Authorization", "Bearer $creatorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun placementApplicationReallocate(
    integrationTestBase: IntegrationTestBase,
    placementApplicationId: UUID,
    body: NewReallocation,
  ) {
    val managerJwt = integrationTestBase.givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)).second

    integrationTestBase.webTestClient.post()
      .uri("/tasks/placement-application/$placementApplicationId/allocations")
      .header("Authorization", "Bearer $managerJwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isCreated
  }

  fun placementRequestWithdraw(
    integrationTestBase: IntegrationTestBase,
    placementRequestId: UUID,
    body: WithdrawPlacementRequest,
  ) {
    val managerJwt = integrationTestBase.givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)).second

    integrationTestBase.webTestClient.post()
      .uri("/placement-requests/$placementRequestId/withdrawal")
      .header("Authorization", "Bearer $managerJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun placementRequestBookingNotMade(
    integrationTestBase: IntegrationTestBase,
    placementRequestId: UUID,
    cruMemberJwt: String,
    body: NewBookingNotMade,
  ) {
    integrationTestBase.webTestClient.post()
      .uri("/placement-requests/$placementRequestId/booking-not-made")
      .header("Authorization", "Bearer $cruMemberJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }

  fun spaceBookingForPlacementRequest(
    integrationTestBase: IntegrationTestBase,
    placementRequestId: UUID,
    managerJwt: String,
    body: Cas1NewSpaceBooking,
  ) = integrationTestBase.webTestClient.post()
    .uri("/cas1/placement-requests/$placementRequestId/space-bookings")
    .header("Authorization", "Bearer $managerJwt")
    .bodyValue(body)
    .exchange()
    .expectStatus()
    .isOk
    .bodyAsObject<Cas1SpaceBooking>()

  fun spaceBookingCancel(
    integrationTestBase: IntegrationTestBase,
    premisesId: UUID,
    bookingId: UUID,
  ) {
    val managerJwt = integrationTestBase.givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)).second
    val reason = integrationTestBase.cancellationReasonEntityFactory.produceAndPersist {
      withServiceScope("approved-premises")
      withIsActive(true)
    }

    integrationTestBase.webTestClient.post()
      .uri("/cas1/premises/$premisesId/space-bookings/$bookingId/cancellations")
      .header("Authorization", "Bearer $managerJwt")
      .bodyValue(
        Cas1NewSpaceBookingCancellation(
          occurredAt = LocalDate.now(),
          reasonId = reason.id,
          reasonNotes = "",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  fun recordArrival(
    integrationTestBase: IntegrationTestBase,
    premisesId: UUID,
    bookingId: UUID,
    arrivalDate: LocalDate,
    arrivalTime: LocalTime,
  ) {
    val managerJwt = integrationTestBase.givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)).second

    integrationTestBase.webTestClient.post()
      .uri("/cas1/premises/$premisesId/space-bookings/$bookingId/arrival")
      .header("Authorization", "Bearer $managerJwt")
      .bodyValue(
        Cas1NewArrival(
          arrivalDate = arrivalDate,
          arrivalTime = arrivalTime.toString(),
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }
}
