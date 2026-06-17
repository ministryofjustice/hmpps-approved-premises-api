package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OASysAssessmentMetadataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.AssessmentInfo

@Service
class Cas2OASysAssessmentInfoTransformer {

  fun toAssessmentMetadata(assessmentInfo: AssessmentInfo?) = assessmentInfo?.let {
    Cas2OASysAssessmentMetadataDto(
      hasApplicableAssessment = true,
      dateStarted = assessmentInfo.initiationDate.toInstant(),
      dateCompleted = assessmentInfo.dateCompleted?.toInstant(),
    )
  } ?: Cas2OASysAssessmentMetadataDto(hasApplicableAssessment = false)
}
