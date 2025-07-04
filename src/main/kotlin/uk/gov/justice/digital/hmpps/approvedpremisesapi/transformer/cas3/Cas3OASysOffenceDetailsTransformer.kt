package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3OASysAssessmentMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails

@Service
class Cas3OASysOffenceDetailsTransformer {
  fun toAssessmentMetadata(offenceDetails: OffenceDetails?) = offenceDetails?.let {
    Cas3OASysAssessmentMetadata(
      hasApplicableAssessment = true,
      dateStarted = offenceDetails.initiationDate.toInstant(),
      dateCompleted = offenceDetails.dateCompleted?.toInstant(),
    )
  } ?: Cas3OASysAssessmentMetadata(hasApplicableAssessment = false)
}
