package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2v2OASysAssessmentMetadataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OASysAssessmentSummary

@Service
class Cas2v2OASysTransformer {

  fun toOASysAssessmentMetadataDto(summary: OASysAssessmentSummary?) = Cas2v2OASysAssessmentMetadataDto(
    hasApplicableAssessment = summary != null,
    dateStarted = summary?.initiationDate?.toInstant(),
    dateCompleted = summary?.completedDate?.toInstant(),
  )
}
