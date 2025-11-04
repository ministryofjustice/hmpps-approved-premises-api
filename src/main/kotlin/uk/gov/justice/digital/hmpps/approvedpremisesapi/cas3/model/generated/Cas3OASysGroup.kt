package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion

/**
 * Groups questions and answers from OAsys
 * @param assessmentMetadata
 * @param answers
 */
data class Cas3OASysGroup(

  val assessmentMetadata: Cas3OASysAssessmentMetadata,

  val answers: List<OASysQuestion>,
)
