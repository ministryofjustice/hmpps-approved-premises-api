package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
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
    val managerJwt = integrationTestBase.`Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)).second

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
    val appealsManagerJwt = integrationTestBase.`Given a User`(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)).second

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
      .uri("/assessments/$assessmentId")
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
      .uri("/assessments/$assessmentId/acceptance")
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
      .uri("/assessments/$assessmentId/rejection")
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
    val managerJwt = integrationTestBase.`Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)).second

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
  ): ClarificationNote {
    return integrationTestBase.webTestClient.post()
      .uri("/assessments/$assessmentId/notes")
      .header("Authorization", "Bearer $assessorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<ClarificationNote>()
      .responseBody
      .blockFirst()!!
  }

  fun assessmentUpdateClarificationNote(
    integrationTestBase: IntegrationTestBase,
    assessmentId: UUID,
    noteId: UUID,
    assessorJwt: String,
    body: UpdatedClarificationNote,
  ) {
    integrationTestBase.webTestClient.put()
      .uri("/assessments/$assessmentId/notes/$noteId")
      .header("Authorization", "Bearer $assessorJwt")
      .bodyValue(body)
      .exchange()
      .expectStatus()
      .isOk
  }
}
