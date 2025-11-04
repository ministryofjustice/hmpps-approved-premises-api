package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 * Groups questions and answers from OAsys. Groups directly align with OAsys Sections other than 'needs', which collates questions from multiple sections
 * @param group
 * @param assessmentMetadata
 * @param answers
 */
data class Cas1OASysGroup(

  val group: Cas1OASysGroupName,

  val assessmentMetadata: Cas1OASysAssessmentMetadata,

  val answers: kotlin.collections.List<OASysQuestion>,
)
