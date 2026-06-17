package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1OASysAssessmentMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OffenceDetails

@Service
class Cas1OASysOffenceDetailsTransformer {
  fun toAssessmentMetadata(offenceDetails: OffenceDetails?) = offenceDetails?.let {
    Cas1OASysAssessmentMetadata(
      hasApplicableAssessment = true,
      dateStarted = offenceDetails.initiationDate.toInstant(),
      dateCompleted = offenceDetails.dateCompleted?.toInstant(),
    )
  } ?: Cas1OASysAssessmentMetadata(hasApplicableAssessment = false)
}
