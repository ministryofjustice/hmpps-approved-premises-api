package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysAssessmentMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails

@Service
class Cas1OASysOffenceDetailsTransformer {
  fun toAssessmentMetadata(offenceDetails: OffenceDetails) = Cas1OASysAssessmentMetadata(
    dateStarted = offenceDetails.initiationDate.toInstant(),
    dateCompleted = offenceDetails.dateCompleted?.toInstant(),
  )
}
