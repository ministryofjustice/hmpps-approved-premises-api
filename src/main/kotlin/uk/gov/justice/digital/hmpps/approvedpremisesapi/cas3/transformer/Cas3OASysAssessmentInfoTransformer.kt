package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3OASysAssessmentMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.AssessmentInfo

@Service
class Cas3OASysAssessmentInfoTransformer {

  fun toAssessmentMetadata(assessmentInfo: AssessmentInfo?) = assessmentInfo?.let {
    Cas3OASysAssessmentMetadata(
      hasApplicableAssessment = true,
      dateStarted = assessmentInfo.initiationDate.toInstant(),
      dateCompleted = assessmentInfo.dateCompleted?.toInstant(),
    )
  } ?: Cas3OASysAssessmentMetadata(hasApplicableAssessment = false)
}
