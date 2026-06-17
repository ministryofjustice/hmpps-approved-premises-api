package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1OASysAssessmentMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.AssessmentInfo

@Service
class Cas1OASysAssessmentInfoTransformer {

  fun toAssessmentMetadata(assessmentInfo: AssessmentInfo?) = assessmentInfo?.let {
    Cas1OASysAssessmentMetadata(
      hasApplicableAssessment = true,
      dateStarted = assessmentInfo.initiationDate.toInstant(),
      dateCompleted = assessmentInfo.dateCompleted?.toInstant(),
    )
  } ?: Cas1OASysAssessmentMetadata(hasApplicableAssessment = false)
}
