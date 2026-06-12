package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2v2OASysAssessmentMetadataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.AssessmentInfo

@Service
class Cas2v2OASysAssessmentInfoTransformer {

  fun toAssessmentMetadata(assessmentInfo: AssessmentInfo?) = assessmentInfo?.let {
    Cas2v2OASysAssessmentMetadataDto(
      hasApplicableAssessment = true,
      dateStarted = assessmentInfo.initiationDate.toInstant(),
      dateCompleted = assessmentInfo.dateCompleted?.toInstant(),
    )
  } ?: Cas2v2OASysAssessmentMetadataDto(hasApplicableAssessment = false)
}
