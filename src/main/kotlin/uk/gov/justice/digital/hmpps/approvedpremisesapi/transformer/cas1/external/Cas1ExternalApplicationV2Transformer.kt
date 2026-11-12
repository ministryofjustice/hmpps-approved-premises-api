package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalAssessmentDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementOutcomeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer

@Component
class Cas1ExternalApplicationV2Transformer(
  private val cas1AssessmentTransformer: Cas1AssessmentTransformer,
  private val cas1ApplicationService: Cas1ApplicationService,
  @Value($$"${url-templates.frontend.application}") private val cas1ApplicationUrlTemplate: String,
) {

  fun transformToApplicationV2(
    application: ApprovedPremisesApplicationEntity,
    upcomingRequestForPlacement: Cas1ExternalRequestForPlacementOutcomeDto?,
    requestForPlacementHistory: List<Cas1ExternalRequestForPlacementOutcomeDto>,
  ) = Cas1ExternalApplicationDto(
    uiUrl = cas1ApplicationUrlTemplate.replace("#id", application.id.toString()),
    id = application.id,
    status = application.status,
    createdAt = application.createdAt,
    createdBy = cas1AssessmentTransformer.transformToStaffDto(application.createdByUser),
    expiresAt = cas1ApplicationService.getApplicationExpiresAt(application),
    submittedApplication = application.submittedAt?.let { submittedAt ->
      Cas1ExternalSubmittedApplicationDto(
        submittedAt = submittedAt,
        assessment = application.getLatestAssessment()?.let {
          Cas1ExternalAssessmentDto(
            decision = cas1AssessmentTransformer.transformJpaDecisionToApi(it.decision),
            rejectionRationale = it.rejectionRationale,
          )
        },
        upcomingRequestForPlacement = upcomingRequestForPlacement,
        requestForPlacementHistory = requestForPlacementHistory,
      )
    },
  )
}
